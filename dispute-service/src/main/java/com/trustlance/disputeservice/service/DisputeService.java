package com.trustlance.disputeservice.service;

import com.trustlance.disputeservice.dto.CreateDisputeRequest;
import com.trustlance.disputeservice.dto.DisputeResponse;
import com.trustlance.disputeservice.dto.ResolveDisputeRequest;
import com.trustlance.disputeservice.dto.SubmitEvidenceRequest;
import com.trustlance.disputeservice.event.DisputeEvent;
import com.trustlance.disputeservice.exception.AppException;
import com.trustlance.disputeservice.model.Dispute;
import com.trustlance.disputeservice.model.DisputeStatus;
import com.trustlance.disputeservice.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String DISPUTE_EVENTS_TOPIC = "dispute-events";

    // ─── Create ───────────────────────────────────────────────────────────────────

    public DisputeResponse createDispute(UUID initiatorId, CreateDisputeRequest request) {
        if (disputeRepository.findByMilestoneId(request.getMilestoneId()).isPresent()) {
            throw new AppException("Dispute already exists for this milestone", HttpStatus.CONFLICT, "DISPUTE_ALREADY_EXISTS");
        }

        Dispute dispute = Dispute.builder()
                .contractId(request.getContractId())
                .milestoneId(request.getMilestoneId())
                .initiatorId(initiatorId)
                .respondentId(request.getRespondentId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(DisputeStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Dispute saved = disputeRepository.save(dispute);

        // Publish DISPUTE_CREATED → escrow-service freezes funds
        publishEvent(saved, "DISPUTE_CREATED");
        log.info("Dispute created for milestone {}", saved.getMilestoneId());

        return toResponse(saved);
    }

    // ─── Get ──────────────────────────────────────────────────────────────────────

    public DisputeResponse getDispute(String disputeId, UUID callerId, boolean isAdmin) {
        Dispute dispute = findById(disputeId);
        if (!isAdmin && !callerId.equals(dispute.getInitiatorId()) && !callerId.equals(dispute.getRespondentId())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        return toResponse(dispute);
    }

    // ─── Get Evidence ─────────────────────────────────────────────────────────────

    public List<Dispute.Evidence> getEvidence(String disputeId, UUID callerId, boolean isAdmin) {
        Dispute dispute = findById(disputeId);
        if (!isAdmin && !callerId.equals(dispute.getInitiatorId()) && !callerId.equals(dispute.getRespondentId())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        return dispute.getEvidenceList();
    }

    // ─── Submit Evidence ──────────────────────────────────────────────────────────

    public DisputeResponse submitEvidence(String disputeId, UUID submitterId, SubmitEvidenceRequest request) {
        Dispute dispute = findById(disputeId);

        if (!submitterId.equals(dispute.getInitiatorId()) && !submitterId.equals(dispute.getRespondentId())) {
            throw new AppException("Only dispute participants can submit evidence", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        if (dispute.getStatus() != DisputeStatus.OPEN
                && dispute.getStatus() != DisputeStatus.EVIDENCE_REQUESTED
                && dispute.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new AppException("Cannot submit evidence for dispute in state: " + dispute.getStatus(),
                    HttpStatus.CONFLICT, "INVALID_STATE");
        }

        Dispute.Evidence evidence = Dispute.Evidence.builder()
                .id(UUID.randomUUID().toString())
                .submitterId(submitterId)
                .notes(request.getNotes())
                .objectStorageRefs(request.getObjectStorageRefs())
                .submittedAt(Instant.now())
                .build();

        dispute.getEvidenceList().add(evidence);
        dispute.setUpdatedAt(Instant.now());

        return toResponse(disputeRepository.save(dispute));
    }

    // ─── Resolve ──────────────────────────────────────────────────────────────────

    public DisputeResponse resolveDispute(String disputeId, UUID adminId, ResolveDisputeRequest request) {
        Dispute dispute = findById(disputeId);

        if (dispute.getStatus() == DisputeStatus.CLOSED) {
            throw new AppException("Dispute is already closed", HttpStatus.CONFLICT, "DISPUTE_CLOSED");
        }

        dispute.setStatus(request.getResolutionStatus());
        dispute.setResolvedById(adminId);
        dispute.setResolutionNotes(request.getResolutionNotes());
        dispute.setUpdatedAt(Instant.now());
        Dispute saved = disputeRepository.save(dispute);

        // Publish DISPUTE_RESOLVED → escrow-service triggers release or refund
        publishEvent(saved, "DISPUTE_RESOLVED");
        log.info("Dispute {} resolved as {}", saved.getId(), saved.getStatus());

        return toResponse(saved);
    }

    // ─── List ─────────────────────────────────────────────────────────────────────

    public Page<DisputeResponse> listMyDisputes(UUID userId, Pageable pageable) {
        return disputeRepository.findByInitiatorIdOrRespondentId(userId, userId, pageable)
                .map(this::toResponse);
    }

    public Page<DisputeResponse> listAllDisputes(Pageable pageable) {
        return disputeRepository.findAll(pageable).map(this::toResponse);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private Dispute findById(String id) {
        return disputeRepository.findById(id)
                .orElseThrow(() -> new AppException("Dispute not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }

    private void publishEvent(Dispute d, String eventType) {
        DisputeEvent event = DisputeEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .producer("dispute-service")
                .occurredAt(Instant.now())
                .disputeId(d.getId())
                .contractId(d.getContractId())
                .milestoneId(d.getMilestoneId())
                .initiatorId(d.getInitiatorId())
                .respondentId(d.getRespondentId())
                .resolutionStatus(d.getStatus() != null ? d.getStatus().name() : null)
                .resolutionNotes(d.getResolutionNotes())
                .build();
        kafkaTemplate.send(DISPUTE_EVENTS_TOPIC, d.getMilestoneId().toString(), event);
    }

    private DisputeResponse toResponse(Dispute d) {
        return DisputeResponse.builder()
                .id(d.getId())
                .contractId(d.getContractId())
                .milestoneId(d.getMilestoneId())
                .initiatorId(d.getInitiatorId())
                .respondentId(d.getRespondentId())
                .title(d.getTitle())
                .description(d.getDescription())
                .status(d.getStatus())
                .resolvedById(d.getResolvedById())
                .resolutionNotes(d.getResolutionNotes())
                .evidenceList(d.getEvidenceList())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

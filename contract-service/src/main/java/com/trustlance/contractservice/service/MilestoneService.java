package com.trustlance.contractservice.service;

import com.trustlance.contractservice.config.UserPrincipal;
import com.trustlance.contractservice.dto.*;
import com.trustlance.contractservice.event.ContractEventPublisher;
import com.trustlance.contractservice.exception.AppException;
import com.trustlance.contractservice.model.*;
import com.trustlance.contractservice.repository.MilestoneRepository;
import com.trustlance.contractservice.repository.StateHistoryRepository;
import com.trustlance.contractservice.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final SubmissionRepository submissionRepository;
    private final StateHistoryRepository stateHistoryRepository;
    private final ContractService contractService;
    private final ContractEventPublisher eventPublisher;

    // ─── Add Milestone ───────────────────────────────────────────────────────────

    @Transactional
    public MilestoneResponse addMilestone(UUID contractId, UUID buyerId, CreateMilestoneRequest request) {
        Contract contract = contractService.findById(contractId);

        // Only the buyer of this contract can add milestones
        if (!contract.getBuyerId().equals(buyerId)) {
            throw new AppException("Only the contract buyer can add milestones", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.DRAFT) {
            throw new AppException("Cannot add milestones to a contract with status: " + contract.getStatus(),
                    HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION");
        }

        Milestone milestone = Milestone.builder()
                .contract(contract)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .deliveryDays(request.getDeliveryDays())
                .status(MilestoneStatus.DRAFT)
                .build();

        Milestone saved = milestoneRepository.save(milestone);
        recordStateHistory(saved.getId(), null, MilestoneStatus.DRAFT.name(), buyerId, "BUYER", null);
        return toResponse(saved);
    }

    // ─── List Milestones ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MilestoneResponse> listMilestones(UUID contractId, UserPrincipal caller) {
        Contract contract = contractService.findById(contractId);
        assertParticipantOrAdmin(contract, caller);
        return milestoneRepository.findByContractId(contractId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Submit Work (SELLER) ────────────────────────────────────────────────────

    @Transactional
    public MilestoneResponse submitWork(UUID milestoneId, UUID sellerId, SubmitMilestoneRequest request) {
        Milestone milestone = findById(milestoneId);
        Contract contract = milestone.getContract();

        // Object-level auth: only the contract's seller can submit
        if (!contract.getSellerId().equals(sellerId)) {
            throw new AppException("Only the contract seller can submit work", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // Valid states from which submission is allowed
        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS
                && milestone.getStatus() != MilestoneStatus.FUNDED
                && milestone.getStatus() != MilestoneStatus.REVISION_REQUESTED) {
            throw new AppException(
                    "Cannot submit work: milestone is in state " + milestone.getStatus(),
                    HttpStatus.CONFLICT, "MILESTONE_STATE_CONFLICT");
        }

        // Record submission (object storage references only — no binary data in MySQL)
        Submission submission = Submission.builder()
                .milestoneId(milestoneId)
                .sellerId(sellerId)
                .notes(request.getNotes())
                .objectStorageRefs(request.getObjectStorageRefs())
                .build();
        submissionRepository.save(submission);

        String prevStatus = milestone.getStatus().name();
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        Milestone saved = milestoneRepository.save(milestone);
        recordStateHistory(saved.getId(), prevStatus, MilestoneStatus.SUBMITTED.name(), sellerId, "SELLER", null);
        eventPublisher.publishMilestoneSubmitted(contract, saved);
        return toResponse(saved);
    }

    // ─── Approve Milestone (BUYER) ───────────────────────────────────────────────

    @Transactional
    public MilestoneResponse approveMilestone(UUID milestoneId, UUID buyerId) {
        Milestone milestone = findById(milestoneId);
        Contract contract = milestone.getContract();

        // Object-level auth: only the contract's buyer can approve
        if (!contract.getBuyerId().equals(buyerId)) {
            throw new AppException("Only the contract buyer can approve milestones", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED
                && milestone.getStatus() != MilestoneStatus.BUYER_REVIEWING) {
            throw new AppException(
                    "Cannot approve milestone in state: " + milestone.getStatus(),
                    HttpStatus.CONFLICT, "MILESTONE_STATE_CONFLICT");
        }

        String prevStatus = milestone.getStatus().name();
        milestone.setStatus(MilestoneStatus.APPROVED);
        Milestone saved = milestoneRepository.save(milestone);
        recordStateHistory(saved.getId(), prevStatus, MilestoneStatus.APPROVED.name(), buyerId, "BUYER", null);
        eventPublisher.publishMilestoneApproved(contract, saved);

        // Check if all milestones are released to complete the contract
        // (RELEASED happens after payment — for now we move to APPROVED and wait for payment service)
        return toResponse(saved);
    }

    // ─── Request Revision (BUYER) ────────────────────────────────────────────────

    @Transactional
    public MilestoneResponse requestRevision(UUID milestoneId, UUID buyerId, RevisionRequest request) {
        Milestone milestone = findById(milestoneId);
        Contract contract = milestone.getContract();

        // Object-level auth: only the contract's buyer can request revision
        if (!contract.getBuyerId().equals(buyerId)) {
            throw new AppException("Only the contract buyer can request revisions", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED
                && milestone.getStatus() != MilestoneStatus.BUYER_REVIEWING) {
            throw new AppException(
                    "Cannot request revision for milestone in state: " + milestone.getStatus(),
                    HttpStatus.CONFLICT, "MILESTONE_STATE_CONFLICT");
        }

        String prevStatus = milestone.getStatus().name();
        milestone.setStatus(MilestoneStatus.REVISION_REQUESTED);
        Milestone saved = milestoneRepository.save(milestone);
        recordStateHistory(saved.getId(), prevStatus, MilestoneStatus.REVISION_REQUESTED.name(),
                buyerId, "BUYER", request.getReason());
        return toResponse(saved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    public Milestone findById(UUID milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new AppException("Milestone not found: " + milestoneId,
                        HttpStatus.NOT_FOUND, "MILESTONE_NOT_FOUND"));
    }

    private void assertParticipantOrAdmin(Contract contract, UserPrincipal caller) {
        boolean isParticipant = contract.getBuyerId().equals(caller.getId())
                || contract.getSellerId().equals(caller.getId());
        boolean isAdmin = "ADMIN".equals(caller.getRole());
        if (!isParticipant && !isAdmin) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }

    private void recordStateHistory(UUID milestoneId, String from, String to, UUID actorId, String actorRole, String reason) {
        StateHistory history = StateHistory.builder()
                .entityType("MILESTONE")
                .entityId(milestoneId)
                .fromStatus(from)
                .toStatus(to)
                .actorId(actorId)
                .actorRole(actorRole)
                .reason(reason)
                .build();
        stateHistoryRepository.save(history);
    }

    // ─── Mapping (inline like marketplace-service) ────────────────────────────────

    public MilestoneResponse toResponse(Milestone m) {
        return MilestoneResponse.builder()
                .id(m.getId())
                .contractId(m.getContract() != null ? m.getContract().getId() : null)
                .title(m.getTitle())
                .description(m.getDescription())
                .amount(m.getAmount())
                .currency(m.getCurrency())
                .deliveryDays(m.getDeliveryDays())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}

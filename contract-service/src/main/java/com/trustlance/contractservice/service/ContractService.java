package com.trustlance.contractservice.service;

import com.trustlance.contractservice.config.UserPrincipal;
import com.trustlance.contractservice.dto.*;
import com.trustlance.contractservice.event.ContractEventPublisher;
import com.trustlance.contractservice.exception.AppException;
import com.trustlance.contractservice.model.*;
import com.trustlance.contractservice.repository.ContractRepository;
import com.trustlance.contractservice.repository.StateHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final StateHistoryRepository stateHistoryRepository;
    private final ContractEventPublisher eventPublisher;

    // ─── Create ─────────────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse createContract(UUID buyerId, CreateContractRequest request) {
        if (request.getSellerId() == null) {
            throw new AppException("Seller ID is required", HttpStatus.BAD_REQUEST, "SELLER_REQUIRED");
        }
        if (buyerId.equals(request.getSellerId())) {
            throw new AppException("Buyer and seller cannot be the same user", HttpStatus.BAD_REQUEST, "INVALID_PARTICIPANTS");
        }

        Contract contract = Contract.builder()
                .buyerId(buyerId)           // always from JWT — never from request body
                .sellerId(request.getSellerId())
                .serviceId(request.getServiceId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ContractStatus.DRAFT)
                .build();

        Contract saved = contractRepository.save(contract);
        recordStateHistory("CONTRACT", saved.getId(), null, ContractStatus.DRAFT.name(), buyerId, "BUYER", null);
        eventPublisher.publishContractCreated(saved);
        return toResponse(saved);
    }

    // ─── Read ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ContractResponse getContract(UUID contractId, UserPrincipal caller) {
        Contract contract = findById(contractId);
        assertParticipantOrAdmin(contract, caller);
        return toResponse(contract);
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> listMyContracts(UUID userId, Pageable pageable) {
        return contractRepository.findByBuyerIdOrSellerId(userId, userId, pageable)
                .map(this::toResponse);
    }

    // ─── State Transitions ───────────────────────────────────────────────────────

    @Transactional
    public ContractResponse acceptContract(UUID contractId, UUID sellerId) {
        Contract contract = findById(contractId);

        // Object-level auth: only the designated seller can accept
        if (!contract.getSellerId().equals(sellerId)) {
            throw new AppException("Only the designated seller can accept this contract", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        assertStatus(contract, ContractStatus.DRAFT, "CONTRACT_NOT_IN_DRAFT");

        contract.setStatus(ContractStatus.ACTIVE);
        Contract saved = contractRepository.save(contract);
        recordStateHistory("CONTRACT", saved.getId(), ContractStatus.DRAFT.name(), ContractStatus.ACTIVE.name(), sellerId, "SELLER", null);
        return toResponse(saved);
    }

    @Transactional
    public ContractResponse cancelContract(UUID contractId, UserPrincipal caller) {
        Contract contract = findById(contractId);
        assertParticipantOrAdmin(contract, caller);

        if (contract.getStatus() == ContractStatus.COMPLETED || contract.getStatus() == ContractStatus.CANCELLED) {
            throw new AppException("Cannot cancel a contract that is already " + contract.getStatus(),
                    HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION");
        }

        String prevStatus = contract.getStatus().name();
        contract.setStatus(ContractStatus.CANCELLED);
        Contract saved = contractRepository.save(contract);
        recordStateHistory("CONTRACT", saved.getId(), prevStatus, ContractStatus.CANCELLED.name(),
                caller.getId(), caller.getRole(), null);
        return toResponse(saved);
    }

    // Called internally when all milestones reach RELEASED
    @Transactional
    public void checkAndCompleteContract(UUID contractId, UUID actorId) {
        Contract contract = findById(contractId);
        if (contract.getStatus() != ContractStatus.ACTIVE) return;

        boolean allReleased = contract.getMilestones().stream()
                .allMatch(m -> m.getStatus() == MilestoneStatus.RELEASED);

        if (allReleased && !contract.getMilestones().isEmpty()) {
            contract.setStatus(ContractStatus.COMPLETED);
            Contract saved = contractRepository.save(contract);
            recordStateHistory("CONTRACT", contractId, ContractStatus.ACTIVE.name(),
                    ContractStatus.COMPLETED.name(), actorId, "SYSTEM", "All milestones released");
            eventPublisher.publishContractCompleted(saved);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    public Contract findById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException("Contract not found: " + contractId, HttpStatus.NOT_FOUND, "CONTRACT_NOT_FOUND"));
    }

    private void assertStatus(Contract contract, ContractStatus expected, String errorCode) {
        if (contract.getStatus() != expected) {
            throw new AppException(
                    "Contract must be in " + expected + " status but is " + contract.getStatus(),
                    HttpStatus.CONFLICT, errorCode);
        }
    }

    private void assertParticipantOrAdmin(Contract contract, UserPrincipal caller) {
        boolean isParticipant = contract.getBuyerId().equals(caller.getId())
                || contract.getSellerId().equals(caller.getId());
        boolean isAdmin = "ADMIN".equals(caller.getRole());
        if (!isParticipant && !isAdmin) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }

    private void recordStateHistory(String entityType, UUID entityId, String from, String to,
                                    UUID actorId, String actorRole, String reason) {
        StateHistory history = StateHistory.builder()
                .entityType(entityType)
                .entityId(entityId)
                .fromStatus(from)
                .toStatus(to)
                .actorId(actorId)
                .actorRole(actorRole)
                .reason(reason)
                .build();
        stateHistoryRepository.save(history);
    }

    // ─── Mapping (no separate mapper class — inline like marketplace-service) ───

    public ContractResponse toResponse(Contract c) {
        return ContractResponse.builder()
                .id(c.getId())
                .buyerId(c.getBuyerId())
                .sellerId(c.getSellerId())
                .serviceId(c.getServiceId())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

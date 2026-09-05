package com.trustlance.escrowservice.service;

import com.trustlance.escrowservice.config.UserPrincipal;
import com.trustlance.escrowservice.dto.DisputeEvent;
import com.trustlance.escrowservice.dto.EscrowResponse;
import com.trustlance.escrowservice.dto.PaymentEvent;
import com.trustlance.escrowservice.event.EscrowEvent;
import com.trustlance.escrowservice.exception.AppException;
import com.trustlance.escrowservice.model.Escrow;
import com.trustlance.escrowservice.model.EscrowStatus;
import com.trustlance.escrowservice.model.EscrowTransaction;
import com.trustlance.escrowservice.repository.EscrowRepository;
import com.trustlance.escrowservice.repository.EscrowTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final EscrowTransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String ESCROW_EVENTS_TOPIC = "escrow-events";
    private static final String LOCK_PREFIX = "escrow:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    // ─── Kafka Consumer ───────────────────────────────────────────────────────────

    @KafkaListener(topics = "payment-events", groupId = "escrow-group",
            containerFactory = "paymentEventKafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        if (!"PAYMENT_SUCCESS".equals(event.getEventType())) {
            return;
        }

        log.info("Received PAYMENT_SUCCESS for milestone {}", event.getMilestoneId());

        // Idempotency: skip if escrow already exists
        if (escrowRepository.findByMilestoneId(event.getMilestoneId()).isPresent()) {
            log.info("Escrow already exists for milestone {}. Skipping.", event.getMilestoneId());
            return;
        }

        Escrow escrow = Escrow.builder()
                .milestoneId(event.getMilestoneId())
                .contractId(event.getContractId())
                .buyerId(event.getBuyerId())
                .sellerId(event.getSellerId() != null ? event.getSellerId() : UUID.randomUUID())
                .amount(event.getAmount())
                .currency(event.getCurrency() != null ? event.getCurrency() : "USD")
                .status(EscrowStatus.FUNDED)
                .build();

        Escrow saved = escrowRepository.save(escrow);
        recordTransaction(saved, null, EscrowStatus.FUNDED, event.getBuyerId(), "SYSTEM",
                "Payment success → escrow funded", "fund:" + event.getMilestoneId());
        publishEvent(saved, "ESCROW_FUNDED");
        log.info("Created FUNDED escrow {} for milestone {}", saved.getId(), saved.getMilestoneId());
    }

    // ─── Read Endpoints ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EscrowResponse getEscrow(UUID escrowId, UserPrincipal caller) {
        Escrow escrow = findById(escrowId);
        assertParticipantOrAdmin(escrow, caller);
        return toResponse(escrow);
    }

    @Transactional(readOnly = true)
    public List<EscrowResponse> getEscrowsByContract(UUID contractId) {
        return escrowRepository.findByContractId(contractId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EscrowResponse> listMyEscrows(UUID userId, Pageable pageable) {
        return escrowRepository.findByBuyerIdOrSellerId(userId, userId, pageable)
                .map(this::toResponse);
    }

    // ─── Release ───────────────────────────────────────────────────────────────────

    @Transactional
    public EscrowResponse releaseFunds(UUID escrowId, UserPrincipal caller) {
        String lockKey = LOCK_PREFIX + escrowId;
        if (!acquireLock(lockKey)) {
            throw new AppException("Another operation is in progress on this escrow", HttpStatus.CONFLICT, "ESCROW_LOCKED");
        }
        try {
            // Re-read state inside the critical section
            Escrow escrow = findById(escrowId);

            if (!escrow.getBuyerId().equals(caller.getId()) && !"ADMIN".equals(caller.getRole())) {
                throw new AppException("Access denied to release funds", HttpStatus.FORBIDDEN, "FORBIDDEN");
            }

            if (escrow.getStatus() != EscrowStatus.FUNDED) {
                throw new AppException("Escrow is not in FUNDED state: " + escrow.getStatus(),
                        HttpStatus.CONFLICT, "INVALID_STATE");
            }

            // Idempotency check
            String idempotencyKey = "release:" + escrowId;
            if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                log.info("Release already processed for escrow {}", escrowId);
                return toResponse(escrow);
            }

            EscrowStatus fromStatus = escrow.getStatus();
            escrow.setStatus(EscrowStatus.RELEASE_PENDING);
            escrowRepository.save(escrow);

            // In production: call external payout API here, then set RELEASED on callback
            escrow.setStatus(EscrowStatus.RELEASED);
            Escrow saved = escrowRepository.save(escrow);

            recordTransaction(saved, fromStatus, EscrowStatus.RELEASED, caller.getId(), caller.getRole(),
                    "Funds released to seller", idempotencyKey);
            publishEvent(saved, "ESCROW_RELEASED");
            log.info("Funds released for escrow {}", saved.getId());

            return toResponse(saved);
        } finally {
            releaseLock(lockKey);
        }
    }

    // ─── Refund ────────────────────────────────────────────────────────────────────

    @Transactional
    public EscrowResponse refundFunds(UUID escrowId, UserPrincipal caller) {
        String lockKey = LOCK_PREFIX + escrowId;
        if (!acquireLock(lockKey)) {
            throw new AppException("Another operation is in progress on this escrow", HttpStatus.CONFLICT, "ESCROW_LOCKED");
        }
        try {
            Escrow escrow = findById(escrowId);

            if (!"ADMIN".equals(caller.getRole())) {
                throw new AppException("Only ADMIN can refund funds", HttpStatus.FORBIDDEN, "FORBIDDEN");
            }

            if (escrow.getStatus() != EscrowStatus.FUNDED && escrow.getStatus() != EscrowStatus.FROZEN) {
                throw new AppException("Escrow is not in a refundable state: " + escrow.getStatus(),
                        HttpStatus.CONFLICT, "INVALID_STATE");
            }

            String idempotencyKey = "refund:" + escrowId;
            if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                return toResponse(escrow);
            }

            EscrowStatus fromStatus = escrow.getStatus();
            escrow.setStatus(EscrowStatus.REFUNDED);
            Escrow saved = escrowRepository.save(escrow);

            recordTransaction(saved, fromStatus, EscrowStatus.REFUNDED, caller.getId(), caller.getRole(),
                    "Funds refunded to buyer", idempotencyKey);
            publishEvent(saved, "ESCROW_REFUNDED");
            log.info("Funds refunded for escrow {}", saved.getId());

            return toResponse(saved);
        } finally {
            releaseLock(lockKey);
        }
    }

    // ─── Freeze (Dispute) ──────────────────────────────────────────────────────────

    @Transactional
    public EscrowResponse freezeFunds(UUID escrowId, UUID actorId) {
        String lockKey = LOCK_PREFIX + escrowId;
        if (!acquireLock(lockKey)) {
            throw new AppException("Another operation is in progress on this escrow", HttpStatus.CONFLICT, "ESCROW_LOCKED");
        }
        try {
            Escrow escrow = findById(escrowId);

            if (escrow.getStatus() != EscrowStatus.FUNDED) {
                throw new AppException("Cannot freeze escrow in state: " + escrow.getStatus(),
                        HttpStatus.CONFLICT, "INVALID_STATE");
            }

            String idempotencyKey = "freeze:" + escrowId;
            if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                return toResponse(escrow);
            }

            EscrowStatus fromStatus = escrow.getStatus();
            escrow.setStatus(EscrowStatus.FROZEN);
            Escrow saved = escrowRepository.save(escrow);

            recordTransaction(saved, fromStatus, EscrowStatus.FROZEN, actorId, "SYSTEM",
                    "Dispute opened - funds frozen", idempotencyKey);
            publishEvent(saved, "ESCROW_FROZEN");
            log.info("Escrow {} frozen for dispute", saved.getId());

            return toResponse(saved);
        } finally {
            releaseLock(lockKey);
        }
    }

    // ─── Kafka Consumer: Dispute Events ────────────────────────────────────────────

    @KafkaListener(topics = "dispute-events", groupId = "escrow-dispute-group",
            containerFactory = "disputeEventKafkaListenerContainerFactory")
    @Transactional
    public void handleDisputeEvent(DisputeEvent event) {
        if (event == null || event.getEventType() == null || event.getMilestoneId() == null) {
            log.warn("Received null or incomplete dispute event, skipping");
            return;
        }

        log.info("Received dispute event {} for milestone {}", event.getEventType(), event.getMilestoneId());

        switch (event.getEventType()) {
            case "DISPUTE_CREATED" -> handleDisputeCreated(event);
            case "DISPUTE_RESOLVED" -> handleDisputeResolved(event);
            default -> log.info("Unhandled dispute event type: {}", event.getEventType());
        }
    }

    private void handleDisputeCreated(DisputeEvent event) {
        escrowRepository.findByMilestoneId(event.getMilestoneId()).ifPresentOrElse(
                escrow -> {
                    if (escrow.getStatus() != EscrowStatus.FUNDED) {
                        log.info("Escrow for milestone {} is in state {}, cannot freeze for dispute",
                                event.getMilestoneId(), escrow.getStatus());
                        return;
                    }

                    String idempotencyKey = "freeze:dispute:" + event.getDisputeId();
                    if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                        log.info("Dispute freeze already processed for disputeId {}", event.getDisputeId());
                        return;
                    }

                    String lockKey = LOCK_PREFIX + escrow.getId();
                    if (!acquireLock(lockKey)) {
                        log.error("Could not acquire lock to freeze escrow {} for dispute {}", escrow.getId(), event.getDisputeId());
                        return;
                    }
                    try {
                        EscrowStatus fromStatus = escrow.getStatus();
                        escrow.setStatus(EscrowStatus.FROZEN);
                        Escrow saved = escrowRepository.save(escrow);

                        recordTransaction(saved, fromStatus, EscrowStatus.FROZEN,
                                event.getInitiatorId(), "SYSTEM",
                                "Dispute opened (id=" + event.getDisputeId() + ") - funds frozen",
                                idempotencyKey);
                        publishEvent(saved, "ESCROW_FROZEN");
                        log.info("Escrow {} frozen due to dispute {}", saved.getId(), event.getDisputeId());
                    } finally {
                        releaseLock(lockKey);
                    }
                },
                () -> log.warn("No escrow found for milestone {} on DISPUTE_CREATED", event.getMilestoneId())
        );
    }

    private void handleDisputeResolved(DisputeEvent event) {
        escrowRepository.findByMilestoneId(event.getMilestoneId()).ifPresentOrElse(
                escrow -> {
                    if (escrow.getStatus() != EscrowStatus.FROZEN) {
                        log.info("Escrow for milestone {} is in state {}, no action needed on DISPUTE_RESOLVED",
                                event.getMilestoneId(), escrow.getStatus());
                        return;
                    }

                    String resolutionStatus = event.getResolutionStatus();
                    if (resolutionStatus == null) {
                        log.warn("DISPUTE_RESOLVED event missing resolutionStatus for dispute {}", event.getDisputeId());
                        return;
                    }

                    String idempotencyKey = "resolve:dispute:" + event.getDisputeId();
                    if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                        log.info("Dispute resolution already processed for disputeId {}", event.getDisputeId());
                        return;
                    }

                    String lockKey = LOCK_PREFIX + escrow.getId();
                    if (!acquireLock(lockKey)) {
                        log.error("Could not acquire lock to resolve escrow {} for dispute {}", escrow.getId(), event.getDisputeId());
                        return;
                    }
                    try {
                        EscrowStatus fromStatus = escrow.getStatus();
                        // BUYER_WINS → refund buyer; SELLER_WINS → release to seller; SPLIT → release (manual split handled externally)
                        EscrowStatus toStatus = switch (resolutionStatus) {
                            case "BUYER_WINS" -> EscrowStatus.REFUNDED;
                            default -> EscrowStatus.RELEASED; // SELLER_WINS or SPLIT
                        };
                        String reason = "Dispute " + event.getDisputeId() + " resolved: " + resolutionStatus
                                + (event.getResolutionNotes() != null ? " — " + event.getResolutionNotes() : "");

                        escrow.setStatus(toStatus);
                        Escrow saved = escrowRepository.save(escrow);

                        recordTransaction(saved, fromStatus, toStatus,
                                event.getInitiatorId(), "ADMIN", reason, idempotencyKey);

                        String kafkaEventType = toStatus == EscrowStatus.REFUNDED ? "ESCROW_REFUNDED" : "ESCROW_RELEASED";
                        publishEvent(saved, kafkaEventType);
                        log.info("Escrow {} {} after dispute resolution ({})", saved.getId(), toStatus, resolutionStatus);
                    } finally {
                        releaseLock(lockKey);
                    }
                },
                () -> log.warn("No escrow found for milestone {} on DISPUTE_RESOLVED", event.getMilestoneId())
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private Escrow findById(UUID id) {
        return escrowRepository.findById(id)
                .orElseThrow(() -> new AppException("Escrow not found: " + id, HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }

    private void assertParticipantOrAdmin(Escrow escrow, UserPrincipal caller) {
        if (!escrow.getBuyerId().equals(caller.getId())
                && !escrow.getSellerId().equals(caller.getId())
                && !"ADMIN".equals(caller.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }

    private void recordTransaction(Escrow escrow, EscrowStatus from, EscrowStatus to,
                                   UUID actorId, String actorRole, String reason, String idempotencyKey) {
        EscrowTransaction tx = EscrowTransaction.builder()
                .escrowId(escrow.getId())
                .milestoneId(escrow.getMilestoneId())
                .fromStatus(from)
                .toStatus(to)
                .amount(escrow.getAmount())
                .currency(escrow.getCurrency())
                .actorId(actorId)
                .actorRole(actorRole)
                .reason(reason)
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(tx);
    }

    private void publishEvent(Escrow escrow, String eventType) {
        EscrowEvent event = EscrowEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .producer("escrow-service")
                .occurredAt(Instant.now())
                .escrowId(escrow.getId())
                .milestoneId(escrow.getMilestoneId())
                .contractId(escrow.getContractId())
                .buyerId(escrow.getBuyerId())
                .sellerId(escrow.getSellerId())
                .amount(escrow.getAmount())
                .currency(escrow.getCurrency())
                .build();
        kafkaTemplate.send(ESCROW_EVENTS_TOPIC, escrow.getMilestoneId().toString(), event);
    }

    // Redis distributed lock — short coordination window
    private boolean acquireLock(String key) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "locked", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    private EscrowResponse toResponse(Escrow e) {
        return EscrowResponse.builder()
                .id(e.getId())
                .milestoneId(e.getMilestoneId())
                .contractId(e.getContractId())
                .buyerId(e.getBuyerId())
                .sellerId(e.getSellerId())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

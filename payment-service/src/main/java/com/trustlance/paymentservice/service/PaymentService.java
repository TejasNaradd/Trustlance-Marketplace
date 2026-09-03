package com.trustlance.paymentservice.service;

import com.trustlance.paymentservice.dto.CreatePaymentIntentRequest;
import com.trustlance.paymentservice.dto.PaymentIntentResponse;
import com.trustlance.paymentservice.event.PaymentEvent;
import com.trustlance.paymentservice.exception.AppException;
import com.trustlance.paymentservice.model.PaymentIntent;
import com.trustlance.paymentservice.model.PaymentStatus;
import com.trustlance.paymentservice.model.WebhookEvent;
import com.trustlance.paymentservice.provider.PaymentProvider;
import com.trustlance.paymentservice.repository.PaymentIntentRepository;
import com.trustlance.paymentservice.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${trustlance.payment.webhook-secret}")
    private String webhookSecret;

    private final PaymentIntentRepository paymentIntentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final List<PaymentProvider> providers;

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    // ─── Create Intent ────────────────────────────────────────────────────────────

    @Transactional
    public PaymentIntentResponse createIntent(UUID buyerId, CreatePaymentIntentRequest request) {
        // Idempotency: return existing intent if same key already exists
        return paymentIntentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    log.info("Returning existing intent for idempotency key {}", request.getIdempotencyKey());
                    return toResponse(existing, Map.of());
                })
                .orElseGet(() -> {
                    PaymentProvider provider = getProvider(request.getProvider());

                    PaymentIntent intent = PaymentIntent.builder()
                            .milestoneId(request.getMilestoneId())
                            .contractId(request.getContractId())
                            .buyerId(buyerId)
                            .amount(request.getAmount())
                            .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                            .provider(provider.getName())
                            .status(PaymentStatus.CREATED)
                            .idempotencyKey(request.getIdempotencyKey())
                            .build();

                    PaymentIntent saved = paymentIntentRepository.save(intent);

                    // Create order with provider
                    PaymentProvider.ProviderOrderResult result = provider.createOrder(saved);
                    saved.setProviderOrderId(result.providerOrderId());
                    saved.setStatus(PaymentStatus.PENDING);
                    paymentIntentRepository.save(saved);

                    return toResponse(saved, result.clientMetadata());
                });
    }

    // ─── Get Intent ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentIntentResponse getIntent(UUID intentId, UUID callerId) {
        PaymentIntent intent = findById(intentId);
        if (!intent.getBuyerId().equals(callerId)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        return toResponse(intent, Map.of());
    }

    @Transactional(readOnly = true)
    public Page<PaymentIntentResponse> listMyPayments(UUID buyerId, Pageable pageable) {
        return paymentIntentRepository.findByBuyerId(buyerId, pageable)
                .map(i -> toResponse(i, Map.of()));
    }

    // ─── Webhook Processing ───────────────────────────────────────────────────────

    @Transactional
    public void processWebhook(String providerName, String rawPayload, String signature) {
        PaymentProvider provider = getProvider(providerName);

        // 1. Verify signature — authoritative proof from provider
        provider.verifyWebhookSignature(rawPayload, signature, webhookSecret);

        // 2. Extract event data
        PaymentProvider.WebhookEventData eventData = provider.extractEventData(rawPayload);

        // 3. Idempotency check — skip if already processed
        if (webhookEventRepository.findByProviderAndProviderEventId(providerName, eventData.providerEventId()).isPresent()) {
            log.info("Webhook already processed: provider={} eventId={}", providerName, eventData.providerEventId());
            return;
        }

        // 4. Persist raw webhook for audit
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .provider(providerName)
                .providerEventId(eventData.providerEventId())
                .eventType(eventData.eventType())
                .rawPayload(rawPayload)
                .processed(false)
                .build();
        webhookEventRepository.save(webhookEvent);

        // 5. Update payment intent state
        try {
            paymentIntentRepository.findByMilestoneIdAndStatus(UUID.randomUUID(), PaymentStatus.PENDING)
                    .ifPresent(intent -> {
                        if (eventData.isSuccess()) {
                            intent.setStatus(PaymentStatus.SUCCESS);
                            intent.setProviderPaymentId(eventData.providerPaymentId());
                            paymentIntentRepository.save(intent);

                            // 6. Publish Kafka event — PAYMENT_SUCCESS
                            publishPaymentEvent(intent, "PAYMENT_SUCCESS", null);
                        } else {
                            intent.setStatus(PaymentStatus.FAILED);
                            paymentIntentRepository.save(intent);
                            publishPaymentEvent(intent, "PAYMENT_FAILED", "Provider reported failure");
                        }
                    });

            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);
        } catch (Exception e) {
            webhookEvent.setProcessingError(e.getMessage());
            webhookEventRepository.save(webhookEvent);
            throw e;
        }
    }

    // ─── Retry ────────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentIntentResponse retryPayment(UUID intentId, UUID buyerId) {
        PaymentIntent intent = findById(intentId);
        if (!intent.getBuyerId().equals(buyerId)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        if (intent.getStatus() != PaymentStatus.FAILED && intent.getStatus() != PaymentStatus.RETRYABLE) {
            throw new AppException("Payment is not in a retryable state: " + intent.getStatus(),
                    HttpStatus.CONFLICT, "PAYMENT_NOT_RETRYABLE");
        }
        if (intent.getRetryCount() >= 3) {
            intent.setStatus(PaymentStatus.FINAL_FAILED);
            paymentIntentRepository.save(intent);
            throw new AppException("Maximum retry attempts reached", HttpStatus.CONFLICT, "MAX_RETRIES_EXCEEDED");
        }

        intent.setRetryCount(intent.getRetryCount() + 1);
        intent.setStatus(PaymentStatus.PENDING);
        PaymentProvider provider = getProvider(intent.getProvider());
        PaymentProvider.ProviderOrderResult result = provider.createOrder(intent);
        intent.setProviderOrderId(result.providerOrderId());
        paymentIntentRepository.save(intent);
        return toResponse(intent, result.clientMetadata());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private PaymentIntent findById(UUID id) {
        return paymentIntentRepository.findById(id)
                .orElseThrow(() -> new AppException("Payment intent not found: " + id, HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND"));
    }

    private PaymentProvider getProvider(String name) {
        return providers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new AppException("Unknown payment provider: " + name, HttpStatus.BAD_REQUEST, "UNKNOWN_PROVIDER"));
    }

    private void publishPaymentEvent(PaymentIntent intent, String eventType, String failureReason) {
        PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .producer("payment-service")
                .occurredAt(Instant.now())
                .paymentIntentId(intent.getId())
                .milestoneId(intent.getMilestoneId())
                .contractId(intent.getContractId())
                .buyerId(intent.getBuyerId())
                .amount(intent.getAmount())
                .currency(intent.getCurrency())
                .provider(intent.getProvider())
                .providerPaymentId(intent.getProviderPaymentId())
                .failureReason(failureReason)
                .build();
        kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, intent.getMilestoneId().toString(), event);
        log.info("Published {} event for milestone {}", eventType, intent.getMilestoneId());
    }

    // ─── Mapping (inline) ─────────────────────────────────────────────────────────

    private PaymentIntentResponse toResponse(PaymentIntent i, Map<String, String> clientMetadata) {
        return PaymentIntentResponse.builder()
                .id(i.getId())
                .milestoneId(i.getMilestoneId())
                .contractId(i.getContractId())
                .buyerId(i.getBuyerId())
                .amount(i.getAmount())
                .currency(i.getCurrency())
                .provider(i.getProvider())
                .providerOrderId(i.getProviderOrderId())
                .status(i.getStatus())
                .clientMetadata(clientMetadata)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}

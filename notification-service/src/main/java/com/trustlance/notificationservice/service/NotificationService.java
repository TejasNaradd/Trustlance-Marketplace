package com.trustlance.notificationservice.service;

import com.trustlance.notificationservice.dto.NotificationEvent;
import com.trustlance.notificationservice.dto.NotificationResponse;
import com.trustlance.notificationservice.exception.AppException;
import com.trustlance.notificationservice.model.DeliveryAttempt;
import com.trustlance.notificationservice.model.Notification;
import com.trustlance.notificationservice.model.NotificationType;
import com.trustlance.notificationservice.model.ProcessedEvent;
import com.trustlance.notificationservice.provider.EmailProvider;
import com.trustlance.notificationservice.repository.DeliveryAttemptRepository;
import com.trustlance.notificationservice.repository.NotificationRepository;
import com.trustlance.notificationservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final List<EmailProvider> emailProviders;

    // ─── Kafka Consumer ───────────────────────────────────────────────────────────

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group")
    public void handleNotificationEvent(NotificationEvent event) {
        // Idempotency check
        String eventId = event.getRecipientId() + ":" + event.getType() + ":" + event.getReferenceId();
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed: {}", eventId);
            return;
        }

        log.info("Processing notification for user {} type {}", event.getRecipientId(), event.getType());

        Notification notification = Notification.builder()
                .recipientId(event.getRecipientId())
                .type(NotificationType.valueOf(event.getType()))
                .title(event.getTitle())
                .message(event.getMessage())
                .referenceId(event.getReferenceId())
                .referenceType(event.getReferenceType())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Record in-app delivery attempt
        recordDeliveryAttempt(saved.getId(), "IN_APP", true, null);

        // Attempt email delivery
        try {
            EmailProvider provider = emailProviders.get(0); // Use first available
            provider.sendEmail(
                    event.getRecipientId().toString(), // In production: lookup user email
                    event.getTitle(),
                    event.getMessage()
            );
            recordDeliveryAttempt(saved.getId(), "EMAIL", true, null);
        } catch (Exception e) {
            recordDeliveryAttempt(saved.getId(), "EMAIL", false, e.getMessage());
            log.warn("Email delivery failed for notification {}: {}", saved.getId(), e.getMessage());
        }

        // Mark event as processed
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(event.getType())
                .processedAt(Instant.now())
                .build());
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────────

    public Page<NotificationResponse> getUserNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
            ? notificationRepository.findByRecipientIdAndReadFalse(userId, pageable)
            : notificationRepository.findByRecipientId(userId, pageable);

        return page.map(this::toResponse);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public NotificationResponse markAsRead(String notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private void recordDeliveryAttempt(String notificationId, String channel, boolean success, String error) {
        deliveryAttemptRepository.save(DeliveryAttempt.builder()
                .notificationId(notificationId)
                .channel(channel)
                .success(success)
                .errorMessage(error)
                .attemptedAt(Instant.now())
                .build());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

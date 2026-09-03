package com.trustlance.aiservice.service;

import com.trustlance.aiservice.dto.GeminiDto;
import com.trustlance.aiservice.dto.VerificationRequest;
import com.trustlance.aiservice.event.AiVerificationEvent;
import com.trustlance.aiservice.exception.AppException;
import com.trustlance.aiservice.model.AiVerification;
import com.trustlance.aiservice.repository.AiVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AiVerificationRepository aiVerificationRepository;
    private final GeminiClient geminiClient;
    private final MinioService minioService;
    private final KafkaTemplate<String, AiVerificationEvent> kafkaTemplate;

    private static final String AI_EVENTS_TOPIC = "ai-events";

    // ─── Submit for Verification ──────────────────────────────────────────────────

    public AiVerification submitForVerification(UUID userId, VerificationRequest request) {
        AiVerification verification = AiVerification.builder()
                .userId(userId)
                .documentType(request.getDocumentType())
                .objectStorageRef(request.getObjectStorageRef())
                .status(AiVerification.VerificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        AiVerification saved = aiVerificationRepository.save(verification);
        log.info("Verification {} created for user {}, document type {}", saved.getId(), userId, request.getDocumentType());

        // Trigger async Gemini AI analysis
        processWithGemini(saved);

        return saved;
    }

    // ─── Async Gemini Processing ──────────────────────────────────────────────────

    @Async
    public void processWithGemini(AiVerification verification) {
        log.info("Starting Gemini AI analysis for verification {}", verification.getId());
        try {
            // 1. Fetch document content from MinIO
            String documentContent;
            try {
                documentContent = minioService.fetchDocumentContent(verification.getObjectStorageRef());
            } catch (Exception e) {
                log.error("Failed to fetch document for verification {}: {}", verification.getId(), e.getMessage());
                markFailed(verification, "Failed to retrieve document from storage: " + e.getMessage());
                return;
            }

            // 2. Call Gemini API
            GeminiDto.VerificationResult result;
            try {
                result = geminiClient.analyzeDocument(
                        verification.getDocumentType(),
                        documentContent,
                        verification.getObjectStorageRef()
                );
            } catch (Exception e) {
                log.error("Gemini API call failed for verification {}: {}", verification.getId(), e.getMessage());
                markFailed(verification, "AI analysis failed: " + e.getMessage());
                return;
            }

            // 3. Map Gemini result to verification status
            AiVerification.VerificationStatus newStatus = switch (result.getStatus()) {
                case "APPROVED" -> AiVerification.VerificationStatus.APPROVED;
                case "REJECTED" -> AiVerification.VerificationStatus.REJECTED;
                default -> AiVerification.VerificationStatus.REQUIRES_MANUAL_REVIEW;
            };

            verification.setStatus(newStatus);
            verification.setConfidenceScore(result.getConfidenceScore());
            verification.setExtractedData(result.getExtractedData());
            if (result.getRejectionReason() != null) {
                // Store rejection reason as part of extracted data JSON prefix
                verification.setExtractedData(
                        "{\"rejectionReason\":\"" + result.getRejectionReason().replace("\"", "'") + "\","
                                + verification.getExtractedData().substring(1));
            }

            AiVerification saved = aiVerificationRepository.save(verification);
            log.info("Gemini analysis complete for verification {}: status={} confidence={}",
                    saved.getId(), newStatus, result.getConfidenceScore());

            // 4. Publish Kafka event so user-service can mark user as verified (if APPROVED)
            publishVerificationEvent(saved);

        } catch (Exception e) {
            log.error("Unexpected error during AI processing for verification {}", verification.getId(), e);
            markFailed(verification, "Unexpected error: " + e.getMessage());
        }
    }

    private void markFailed(AiVerification verification, String reason) {
        verification.setStatus(AiVerification.VerificationStatus.REQUIRES_MANUAL_REVIEW);
        verification.setExtractedData("{\"processingError\":\"" + reason.replace("\"", "'") + "\"}");
        verification.setConfidenceScore(0.0);
        aiVerificationRepository.save(verification);
    }

    private void publishVerificationEvent(AiVerification verification) {
        try {
            AiVerificationEvent event = AiVerificationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("VERIFICATION_COMPLETED")
                    .producer("ai-service")
                    .occurredAt(Instant.now())
                    .verificationId(verification.getId())
                    .userId(verification.getUserId())
                    .documentType(verification.getDocumentType())
                    .status(verification.getStatus().name())
                    .confidenceScore(verification.getConfidenceScore())
                    .build();

            kafkaTemplate.send(AI_EVENTS_TOPIC, verification.getUserId().toString(), event);
            log.info("Published VERIFICATION_COMPLETED event for verification {}", verification.getId());
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event for verification {}: {}", verification.getId(), e.getMessage());
        }
    }

    // ─── Read Endpoints ───────────────────────────────────────────────────────────

    public Page<AiVerification> getMyVerifications(UUID userId, Pageable pageable) {
        return aiVerificationRepository.findByUserId(userId, pageable);
    }

    public AiVerification getVerification(String id, UUID callerId, boolean isAdmin) {
        AiVerification verification = aiVerificationRepository.findById(id)
                .orElseThrow(() -> new AppException("Verification not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));

        if (!isAdmin && !verification.getUserId().equals(callerId)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        return verification;
    }
}

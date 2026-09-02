package com.trustlance.aiservice.service;

import com.trustlance.aiservice.dto.VerificationRequest;
import com.trustlance.aiservice.exception.AppException;
import com.trustlance.aiservice.model.AiVerification;
import com.trustlance.aiservice.repository.AiVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AiVerificationRepository aiVerificationRepository;

    public AiVerification submitForVerification(UUID userId, VerificationRequest request) {
        AiVerification verification = AiVerification.builder()
                .userId(userId)
                .documentType(request.getDocumentType())
                .objectStorageRef(request.getObjectStorageRef())
                .status(AiVerification.VerificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();
                
        AiVerification saved = aiVerificationRepository.save(verification);
        
        // Asynchronously trigger AI models
        simulateAiProcessing(saved);
        
        return saved;
    }
    
    private void simulateAiProcessing(AiVerification verification) {
        // In a real application, we would call an AI model (like Google Cloud Vision or a custom LLM)
        // using Spring AI or REST templates to analyze the document.
        // For now, we simulate processing.
        
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate processing time
                
                verification.setStatus(AiVerification.VerificationStatus.APPROVED);
                verification.setConfidenceScore(0.95);
                verification.setExtractedData("{\"name\": \"John Doe\", \"id_number\": \"123456789\"}");
                
                aiVerificationRepository.save(verification);
                log.info("AI Verification completed for {}", verification.getId());
                
                // Publish Kafka event to user-service to mark user as verified
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
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

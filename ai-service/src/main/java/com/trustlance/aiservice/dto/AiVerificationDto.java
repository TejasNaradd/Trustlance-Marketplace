package com.trustlance.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified DTOs for AI verification results.
 * Model-agnostic structure that works with any LLM provider (Groq, OpenAI, Anthropic, etc.)
 */
public class AiVerificationDto {

    /**
     * Verification result returned by AI model.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerificationResult {
        /** Verification status: APPROVED | REJECTED | REQUIRES_MANUAL_REVIEW */
        private String status;

        /** AI confidence score (0.0 - 1.0) */
        private double confidenceScore;

        /** Extracted data as JSON string */
        private String extractedData;

        /** Rejection reason (only if status is REJECTED) */
        private String rejectionReason;
    }
}

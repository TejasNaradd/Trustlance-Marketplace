package com.trustlance.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request and response DTOs for the Gemini generateContent REST API.
 */
public class GeminiDto {

    // ─── Request ──────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateContentRequest {
        private List<Content> contents;
        @JsonProperty("generationConfig")
        private GenerationConfig generationConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        @JsonProperty("responseMimeType")
        private String responseMimeType = "application/json";
        @JsonProperty("maxOutputTokens")
        private int maxOutputTokens = 1024;
        private double temperature = 0.1;
    }

    // ─── Response ─────────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateContentResponse {
        private List<Candidate> candidates;
        @JsonProperty("usageMetadata")
        private UsageMetadata usageMetadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
        @JsonProperty("finishReason")
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private int promptTokenCount;
        @JsonProperty("candidatesTokenCount")
        private int candidatesTokenCount;
    }

    // ─── Extracted Verification Result ────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerificationResult {
        private String status;           // APPROVED | REJECTED | REQUIRES_MANUAL_REVIEW
        private double confidenceScore;  // 0.0 – 1.0
        private String extractedData;    // JSON string with extracted fields
        private String rejectionReason;  // only for REJECTED
    }
}

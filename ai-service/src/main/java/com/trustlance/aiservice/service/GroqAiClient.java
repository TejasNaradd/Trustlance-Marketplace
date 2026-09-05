package com.trustlance.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustlance.aiservice.dto.AiVerificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring AI-based client for Groq LLM document verification.
 * Uses OpenAI-compatible API via Spring AI abstractions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqAiClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.options.temperature:0.1}")
    private double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1024}")
    private int maxTokens;

    /**
     * Analyzes a document using Groq AI and returns structured verification result.
     *
     * @param documentType     the type of document (ID_CARD, PASSPORT, INVOICE, etc.)
     * @param documentContent  base64-encoded or text content
     * @param objectStorageRef MinIO reference URL
     * @return parsed VerificationResult
     */
    public AiVerificationDto.VerificationResult analyzeDocument(
            String documentType,
            String documentContent,
            String objectStorageRef) {

        String prompt = buildPrompt(documentType, documentContent, objectStorageRef);

        log.debug("Sending document analysis prompt to Groq AI (type: {})", documentType);

        try {
            // Call Groq via Spring AI
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Received Groq AI response: {}", response);

            return parseAiResponse(response);

        } catch (Exception ex) {
            log.error("Groq AI analysis failed for document type {}: {}", documentType, ex.getMessage(), ex);
            throw new RuntimeException("AI analysis failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Builds the prompt for document verification.
     */
    private String buildPrompt(String documentType, String documentContent, String objectStorageRef) {
        return """
                You are a document verification AI for a freelance marketplace platform (TrustLance).
                Analyze the following %s document and extract key information for identity verification.
                
                Document reference: %s
                Document content/description:
                %s
                
                Respond with ONLY a valid JSON object in this exact format (no markdown, no extra text):
                {
                  "status": "APPROVED" | "REJECTED" | "REQUIRES_MANUAL_REVIEW",
                  "confidenceScore": <number between 0.0 and 1.0>,
                  "extractedData": {
                    "documentType": "<type>",
                    "name": "<full name if found>",
                    "idNumber": "<ID or document number if found>",
                    "expiryDate": "<expiry date if found or null>",
                    "issuingCountry": "<country if found or null>",
                    "additionalFields": {}
                  },
                  "rejectionReason": "<reason if REJECTED, otherwise null>"
                }
                
                Verification Rules:
                - Set status to APPROVED if the document appears legitimate, not expired, and all required fields are present.
                - Set status to REJECTED if the document is clearly invalid, expired, tampered, or fraudulent.
                - Set status to REQUIRES_MANUAL_REVIEW if uncertain or if critical fields are missing.
                - confidenceScore must reflect certainty: 0.9+ for clear cases, 0.5-0.89 for uncertain, <0.5 for problematic.
                - For expired documents, set status to REJECTED with rejectionReason explaining expiry.
                - Respond ONLY with the JSON object, no additional text or formatting.
                """.formatted(documentType, objectStorageRef, documentContent);
    }

    /**
     * Parses Groq AI JSON response into VerificationResult DTO.
     */
    private AiVerificationDto.VerificationResult parseAiResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("Empty response from Groq AI");
            return buildFallbackResult("Empty response from AI model");
        }

        // Clean potential markdown formatting (some models add ```json ... ```)
        String cleanedJson = rawResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*$", "")
                .trim();

        try {
            JsonNode node = objectMapper.readTree(cleanedJson);

            String status = node.path("status").asText("REQUIRES_MANUAL_REVIEW");
            double confidence = node.path("confidenceScore").asDouble(0.5);
            String extractedData = objectMapper.writeValueAsString(node.path("extractedData"));
            String rejectionReason = node.path("rejectionReason").isNull() 
                    ? null 
                    : node.path("rejectionReason").asText();

            // Clamp confidence to [0.0, 1.0]
            confidence = Math.max(0.0, Math.min(1.0, confidence));

            return AiVerificationDto.VerificationResult.builder()
                    .status(status)
                    .confidenceScore(confidence)
                    .extractedData(extractedData)
                    .rejectionReason(rejectionReason)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Groq AI response as JSON. Raw response: {}", cleanedJson, e);
            return buildFallbackResult("Failed to parse AI response: " + e.getMessage());
        }
    }

    /**
     * Builds a fallback result when AI processing fails.
     */
    private AiVerificationDto.VerificationResult buildFallbackResult(String errorMessage) {
        return AiVerificationDto.VerificationResult.builder()
                .status("REQUIRES_MANUAL_REVIEW")
                .confidenceScore(0.0)
                .extractedData("{\"processingError\":\"" + errorMessage.replace("\"", "'") + "\"}")
                .rejectionReason(errorMessage)
                .build();
    }
}

package com.trustlance.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustlance.aiservice.config.GeminiProperties;
import com.trustlance.aiservice.dto.GeminiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * HTTP client for Google Gemini generateContent API.
 * Sends a text prompt with base64-encoded document content and returns structured JSON.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    /**
     * Sends a document analysis prompt to Gemini and returns a parsed VerificationResult.
     *
     * @param documentType  the type of document (ID_CARD, PASSPORT, INVOICE, etc.)
     * @param documentContent base64-encoded or raw text content of the document
     * @param objectStorageRef original storage URL for context
     * @return parsed VerificationResult
     */
    public GeminiDto.VerificationResult analyzeDocument(String documentType,
                                                         String documentContent,
                                                         String objectStorageRef) {
        String prompt = buildPrompt(documentType, documentContent, objectStorageRef);

        GeminiDto.GenerateContentRequest requestBody = GeminiDto.GenerateContentRequest.builder()
                .contents(List.of(
                        GeminiDto.Content.builder()
                                .parts(List.of(GeminiDto.Part.builder().text(prompt).build()))
                                .build()
                ))
                .generationConfig(GeminiDto.GenerationConfig.builder()
                        .responseMimeType("application/json")
                        .maxOutputTokens(1024)
                        .temperature(0.1)
                        .build())
                .build();

        String url = "/" + geminiProperties.getModel() + ":generateContent?key=" + geminiProperties.getApiKey();

        try {
            GeminiDto.GenerateContentResponse response = geminiWebClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GeminiDto.GenerateContentResponse.class)
                    .block();

            return parseGeminiResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("Gemini API error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Gemini API call failed: " + ex.getMessage(), ex);
        }
    }

    private String buildPrompt(String documentType, String documentContent, String objectStorageRef) {
        return """
                You are a document verification AI for a freelance marketplace platform.
                Analyze the following %s document and extract key information.
                
                Document reference: %s
                Document content / description:
                %s
                
                Respond with a JSON object in exactly this format:
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
                
                Rules:
                - Set status to APPROVED if the document appears legitimate and all required fields are present.
                - Set status to REJECTED if the document is clearly invalid, expired, or fraudulent.
                - Set status to REQUIRES_MANUAL_REVIEW if uncertain or if critical fields are missing.
                - confidenceScore must reflect how certain you are: 0.9+ for clear cases, 0.5-0.89 for uncertain.
                - Do not include any text outside the JSON object.
                """.formatted(documentType, objectStorageRef, documentContent);
    }

    private GeminiDto.VerificationResult parseGeminiResponse(GeminiDto.GenerateContentResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            log.warn("Empty response from Gemini API");
            return GeminiDto.VerificationResult.builder()
                    .status("REQUIRES_MANUAL_REVIEW")
                    .confidenceScore(0.0)
                    .extractedData("{}")
                    .rejectionReason("Empty response from AI model")
                    .build();
        }

        GeminiDto.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null
                || candidate.getContent().getParts().isEmpty()) {
            log.warn("No content parts in Gemini response");
            return GeminiDto.VerificationResult.builder()
                    .status("REQUIRES_MANUAL_REVIEW")
                    .confidenceScore(0.0)
                    .extractedData("{}")
                    .rejectionReason("No content in AI response")
                    .build();
        }

        String rawJson = candidate.getContent().getParts().get(0).getText();
        log.debug("Raw Gemini response JSON: {}", rawJson);

        try {
            var node = objectMapper.readTree(rawJson);
            String status = node.path("status").asText("REQUIRES_MANUAL_REVIEW");
            double confidence = node.path("confidenceScore").asDouble(0.5);
            String extractedData = objectMapper.writeValueAsString(node.path("extractedData"));
            String rejectionReason = node.path("rejectionReason").isNull() ? null
                    : node.path("rejectionReason").asText();

            // Clamp confidence
            confidence = Math.max(0.0, Math.min(1.0, confidence));

            return GeminiDto.VerificationResult.builder()
                    .status(status)
                    .confidenceScore(confidence)
                    .extractedData(extractedData)
                    .rejectionReason(rejectionReason)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response JSON: {}", rawJson, e);
            return GeminiDto.VerificationResult.builder()
                    .status("REQUIRES_MANUAL_REVIEW")
                    .confidenceScore(0.0)
                    .extractedData("{\"raw\": " + objectMapper.createObjectNode().textNode(rawJson) + "}")
                    .rejectionReason("Failed to parse AI response")
                    .build();
        }
    }
}

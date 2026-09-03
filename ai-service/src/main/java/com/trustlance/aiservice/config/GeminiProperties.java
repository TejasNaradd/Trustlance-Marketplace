package com.trustlance.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "trustlance.ai.gemini")
@Data
public class GeminiProperties {

    /** Gemini API key — injected from env GEMINI_API_KEY */
    private String apiKey;

    /** Model to use: gemini-2.0-flash, gemini-1.5-pro, etc. */
    private String model = "gemini-2.0-flash";

    /** Base URL for the Gemini REST API */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models";
}

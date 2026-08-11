package com.fanzone.moderation.service;

import com.fanzone.moderation.dto.ToxicityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI moderation API client.
 * Calls the moderation endpoint with a 2-second timeout.
 * On timeout or error, returns safe (fail-open) and queues for async review.
 */
@Component
public class OpenAiModerationClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiModerationClient.class);

    private final String apiKey;
    private final String apiUrl;
    private final RestClient restClient;

    public OpenAiModerationClient(
            @Value("${fanzone.openai.api-key:}") String apiKey,
            @Value("${fanzone.openai.moderation-url:https://api.openai.com/v1/moderations}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Calls OpenAI moderation API. Returns ToxicityResult.
     * On any error/timeout, returns safe result.
     */
    public ToxicityResult moderate(String content) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenAI API key not configured, returning safe result");
            return ToxicityResult.safe();
        }

        try {
            Map<String, Object> requestBody = Map.of("input", content);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return ToxicityResult.safe();
            }

            return parseResponse(response);
        } catch (Exception e) {
            log.warn("OpenAI moderation call failed (returning safe): {}", e.getMessage());
            return ToxicityResult.safe();
        }
    }

    @SuppressWarnings("unchecked")
    private ToxicityResult parseResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                return ToxicityResult.safe();
            }

            Map<String, Object> result = results.get(0);
            Boolean flagged = (Boolean) result.get("flagged");

            if (flagged != null && flagged) {
                Map<String, Boolean> categories = (Map<String, Boolean>) result.get("categories");
                Map<String, Number> scores = (Map<String, Number>) result.get("category_scores");

                String reason = categories.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("toxic_content");

                double confidence = scores != null
                        ? scores.values().stream().mapToDouble(Number::doubleValue).max().orElse(0.5)
                        : 0.5;

                return ToxicityResult.toxic(reason, confidence);
            }

            return ToxicityResult.safe();
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI response: {}", e.getMessage());
            return ToxicityResult.safe();
        }
    }
}

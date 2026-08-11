package com.fanzone.moderation.service;

import com.fanzone.common.events.KafkaTopics;
import com.fanzone.moderation.dto.ToxicityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Moderation service implementation.
 * Calls OpenAI moderation with 2s timeout.
 * On failure/timeout: returns safe immediately, queues content for async review.
 */
@Service
public class ModerationServiceImpl implements ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationServiceImpl.class);

    private final OpenAiModerationClient openAiClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ModerationServiceImpl(OpenAiModerationClient openAiClient,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.openAiClient = openAiClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public ToxicityResult analyzeContent(String content) {
        try {
            ToxicityResult result = openAiClient.moderate(content);
            log.debug("Moderation result: toxic={}, reason={}", result.isToxic(), result.reason());
            return result;
        } catch (Exception e) {
            log.warn("Moderation analysis failed, returning safe and queuing for async review: {}",
                    e.getMessage());
            queueForAsyncReview(content);
            return ToxicityResult.safe();
        }
    }

    private void queueForAsyncReview(String content) {
        try {
            kafkaTemplate.send(KafkaTopics.MODERATION_REQUESTS, Map.of("content", content, "reason", "timeout_fallback"));
        } catch (Exception e) {
            log.error("Failed to queue content for async moderation review: {}", e.getMessage());
        }
    }
}

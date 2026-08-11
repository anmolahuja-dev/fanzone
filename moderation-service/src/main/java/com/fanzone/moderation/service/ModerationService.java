package com.fanzone.moderation.service;

import com.fanzone.moderation.dto.ToxicityResult;

public interface ModerationService {

    /**
     * Analyzes content for toxicity via OpenAI API.
     * Returns within 2 seconds. On timeout → returns safe, queues for async review.
     * Does NOT flag football performance criticism without personal attacks.
     */
    ToxicityResult analyzeContent(String content);
}

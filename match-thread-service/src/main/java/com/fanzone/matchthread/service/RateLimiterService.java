package com.fanzone.matchthread.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Rate limiter using Redis sorted sets.
 * Enforces max 20 comments per user per match per 60-second rolling window.
 */
@Service
@ConditionalOnBean(RedisConnectionFactory.class)
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final int MAX_COMMENTS_PER_WINDOW = 20;
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if the user is allowed to post another comment (under rate limit).
     */
    public boolean isAllowed(UUID matchId, UUID userId) {
        String key = rateKey(matchId, userId);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (WINDOW_SECONDS * 1000);

        try {
            // Remove entries outside the window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // Count entries in window
            Long count = redisTemplate.opsForZSet().zCard(key);
            return count == null || count < MAX_COMMENTS_PER_WINDOW;
        } catch (Exception e) {
            log.warn("Rate limiter check failed for user {} match {}: {}", userId, matchId, e.getMessage());
            return true; // Fail open
        }
    }

    /**
     * Records a comment timestamp for rate limiting.
     */
    public void recordComment(UUID matchId, UUID userId) {
        String key = rateKey(matchId, userId);
        long now = Instant.now().toEpochMilli();

        try {
            redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
            // Expire the key after the window to avoid stale data
            redisTemplate.expire(key, java.time.Duration.ofSeconds(WINDOW_SECONDS + 5));
        } catch (Exception e) {
            log.warn("Rate limiter record failed for user {} match {}: {}", userId, matchId, e.getMessage());
        }
    }

    private String rateKey(UUID matchId, UUID userId) {
        return "rate:" + matchId + ":" + userId;
    }

    // Exposed for testing
    public static int getMaxCommentsPerWindow() {
        return MAX_COMMENTS_PER_WINDOW;
    }

    public static long getWindowSeconds() {
        return WINDOW_SECONDS;
    }
}

package com.fanzone.matchthread.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Fallback rate limiter that always allows (for dev/test without Redis).
 */
@Service
@ConditionalOnMissingBean(RedisConnectionFactory.class)
public class NoOpRateLimiterService extends RateLimiterService {

    public NoOpRateLimiterService() {
        super(null);
    }

    @Override
    public boolean isAllowed(UUID matchId, UUID userId) {
        return true;
    }

    @Override
    public void recordComment(UUID matchId, UUID userId) {
        // no-op
    }
}

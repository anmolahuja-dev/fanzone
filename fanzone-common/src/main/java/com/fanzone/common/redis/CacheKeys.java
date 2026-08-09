package com.fanzone.common.redis;

import java.util.UUID;

/**
 * Utility class for generating consistent Redis cache keys.
 * All keys are prefixed with "fanzone:" to namespace the application's cache entries.
 */
public final class CacheKeys {

    public static final String PREFIX = "fanzone:";

    private CacheKeys() {
        // Utility class - no instantiation
    }

    /**
     * Generates a feed cache key.
     * Format: fanzone:feed:{userId}:{tab}
     *
     * @param userId the user's ID
     * @param tab    the feed tab (e.g., "home", "following", "trending")
     * @return the cache key
     */
    public static String feedKey(UUID userId, String tab) {
        return PREFIX + "feed:" + userId + ":" + tab;
    }

    /**
     * Generates a rate-limiting cache key for match interactions.
     * Format: fanzone:rate:{matchId}:{userId}
     *
     * @param matchId the match ID
     * @param userId  the user's ID
     * @return the cache key
     */
    public static String rateKey(UUID matchId, UUID userId) {
        return PREFIX + "rate:" + matchId + ":" + userId;
    }

    /**
     * Generates a session cache key.
     * Format: fanzone:session:{userId}
     *
     * @param userId the user's ID
     * @return the cache key
     */
    public static String sessionKey(UUID userId) {
        return PREFIX + "session:" + userId;
    }

    /**
     * Generates a user cache key.
     * Format: fanzone:user:{userId}
     *
     * @param userId the user's ID
     * @return the cache key
     */
    public static String userCacheKey(UUID userId) {
        return PREFIX + "user:" + userId;
    }
}

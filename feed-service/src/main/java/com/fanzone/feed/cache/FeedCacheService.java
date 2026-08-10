package com.fanzone.feed.cache;

import com.fanzone.common.redis.CacheKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for caching "For You" feed data in Redis sorted sets.
 * <p>
 * Key format: fanzone:feed:{userId}:for-you
 * Score: the feed score computed by FeedScorer
 * Member: post ID as string
 * TTL: 5 minutes per cached feed
 */
@Service
@ConditionalOnBean(RedisConnectionFactory.class)
public class FeedCacheService {

    private static final Logger log = LoggerFactory.getLogger(FeedCacheService.class);
    private static final Duration FEED_CACHE_TTL = Duration.ofMinutes(5);
    private static final String FOR_YOU_TAB = "for-you";

    private final StringRedisTemplate redisTemplate;

    public FeedCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Caches a scored feed page for a user in a Redis sorted set.
     * The sorted set stores post IDs with their feed scores, allowing
     * efficient range queries for pagination.
     *
     * @param userId the user ID
     * @param tab    the feed tab identifier (e.g., "for-you")
     * @param posts  the list of scored posts to cache
     */
    public void cacheFeed(UUID userId, String tab, List<ScoredPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        String key = CacheKeys.feedKey(userId, tab);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = new java.util.HashSet<>();
            for (ScoredPost post : posts) {
                tuples.add(ZSetOperations.TypedTuple.of(post.postId(), post.score()));
            }
            zSetOps.add(key, tuples);
            redisTemplate.expire(key, FEED_CACHE_TTL);
            log.debug("Cached {} posts for user {} tab {}", posts.size(), userId, tab);
        } catch (Exception e) {
            log.warn("Failed to cache feed for user {} tab {}: {}", userId, tab, e.getMessage());
        }
    }

    /**
     * Retrieves cached post IDs for a user's feed page, ordered by score descending.
     * Returns empty Optional on cache miss (key doesn't exist or Redis error).
     *
     * @param userId the user ID
     * @param tab    the feed tab identifier
     * @param offset the pagination offset (0-based)
     * @param size   the page size
     * @return Optional containing a list of post IDs if cache hit, empty if cache miss
     */
    public Optional<List<String>> getCachedPostIds(UUID userId, String tab, int offset, int size) {
        String key = CacheKeys.feedKey(userId, tab);

        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (exists == null || !exists) {
                return Optional.empty();
            }

            // reverseRange returns members ordered from highest to lowest score
            Set<String> postIds = redisTemplate.opsForZSet()
                    .reverseRange(key, offset, (long) offset + size - 1);

            if (postIds == null || postIds.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(List.copyOf(postIds));
        } catch (Exception e) {
            log.warn("Failed to read cached feed for user {} tab {}: {}", userId, tab, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Invalidates the cached feed for all users of a specific club.
     * For MVP, this uses a pattern-based key scan to find and delete matching feed keys.
     * Note: In production, consider maintaining a club→users mapping to avoid SCAN overhead.
     *
     * @param clubId the club ID whose users' feed caches should be invalidated
     */
    public void invalidateForClub(UUID clubId) {
        try {
            // For MVP: scan for all feed keys and delete them.
            // A more targeted approach would maintain a set of users per club,
            // but for MVP the simpler pattern-based scan is acceptable.
            String pattern = CacheKeys.PREFIX + "feed:*:" + FOR_YOU_TAB;
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("Invalidated {} feed cache entries for club {}", deleted, clubId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate feed cache for club {}: {}", clubId, e.getMessage());
        }
    }

    /**
     * Invalidates the cached feed for a specific user.
     *
     * @param userId the user ID whose feed cache should be invalidated
     */
    public void invalidateForUser(UUID userId) {
        try {
            String key = CacheKeys.feedKey(userId, FOR_YOU_TAB);
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Invalidated feed cache for user {}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate feed cache for user {}: {}", userId, e.getMessage());
        }
    }
}

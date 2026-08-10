package com.fanzone.feed.cache;

/**
 * Represents a post with its computed feed score.
 * Used as the unit of data stored in the Redis sorted set cache.
 *
 * @param postId the unique post identifier
 * @param score  the computed feed score (from FeedScorer)
 */
public record ScoredPost(
        String postId,
        double score
) {}

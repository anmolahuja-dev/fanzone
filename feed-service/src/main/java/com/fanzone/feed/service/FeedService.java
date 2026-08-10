package com.fanzone.feed.service;

import com.fanzone.common.dto.CursorPage;
import com.fanzone.feed.dto.PostDto;

import java.util.UUID;

/**
 * Service interface for feed retrieval with cursor-based pagination.
 * Each method returns a page of posts enriched with author info.
 */
public interface FeedService {

    /**
     * Get the "For You" personalized feed.
     * Posts are ranked by composite score (compute_feed_score) DESC, then created_at DESC.
     *
     * @param userId     the current user's ID
     * @param userClubId the user's favorite club ID (for scoring)
     * @param cursor     the last post ID from previous page (null for first page)
     * @param size       number of posts per page (default 20)
     * @return paginated posts with cursor for next page
     */
    CursorPage<PostDto> getForYouFeed(UUID userId, UUID userClubId, String cursor, int size);

    /**
     * Get the "Club" feed tab.
     * Shows only posts from the user's favorite club, ordered by recency.
     *
     * @param userClubId the user's favorite club ID to filter by
     * @param cursor     the last post ID from previous page (null for first page)
     * @param size       number of posts per page (default 20)
     * @return paginated posts with cursor for next page
     */
    CursorPage<PostDto> getClubFeed(UUID userClubId, String cursor, int size);

    /**
     * Get the "Following" feed tab.
     * Shows only posts from users the current user follows, ordered by recency.
     *
     * @param userId the current user's ID (to look up followed users)
     * @param cursor the last post ID from previous page (null for first page)
     * @param size   number of posts per page (default 20)
     * @return paginated posts with cursor for next page
     */
    CursorPage<PostDto> getFollowingFeed(UUID userId, String cursor, int size);
}

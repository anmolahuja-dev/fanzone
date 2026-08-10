package com.fanzone.feed.controller;

import com.fanzone.common.dto.CursorPage;
import com.fanzone.common.security.UserPrincipal;
import com.fanzone.feed.dto.PostDto;
import com.fanzone.feed.service.FeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for feed retrieval.
 * <p>
 * Provides three feed tabs with cursor-based pagination:
 * - For You: personalized feed ranked by composite score
 * - Club: posts from the user's favorite club, ordered by recency
 * - Following: posts from followed users, ordered by recency
 * <p>
 * All endpoints require authentication via JWT Bearer token.
 */
@RestController
@RequestMapping("/api/v1/feeds")
public class FeedController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * Get the "For You" personalized feed.
     * Posts are ranked by a composite score (same club bonus, reputation bonus,
     * recency bonus, engagement bonus, toxic penalty) then by created_at descending.
     *
     * @param principal the authenticated user (injected from JWT)
     * @param cursor    opaque cursor from previous page's nextCursor (null for first page)
     * @param size      number of posts per page (default 20, max 100)
     * @return paginated posts with cursor for next page
     */
    @GetMapping("/for-you")
    public ResponseEntity<CursorPage<PostDto>> getForYouFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<PostDto> page = feedService.getForYouFeed(
                principal.getUserId(),
                principal.getFavoriteClubId(),
                cursor,
                size
        );

        return ResponseEntity.ok(page);
    }

    /**
     * Get the "Club" feed tab.
     * Shows only posts associated with the user's favorite club, ordered by recency.
     *
     * @param principal the authenticated user (injected from JWT)
     * @param cursor    opaque cursor from previous page's nextCursor (null for first page)
     * @param size      number of posts per page (default 20, max 100)
     * @return paginated posts with cursor for next page
     */
    @GetMapping("/club")
    public ResponseEntity<CursorPage<PostDto>> getClubFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<PostDto> page = feedService.getClubFeed(
                principal.getFavoriteClubId(),
                cursor,
                size
        );

        return ResponseEntity.ok(page);
    }

    /**
     * Get the "Following" feed tab.
     * Shows only posts from users the current user follows, ordered by recency.
     *
     * @param principal the authenticated user (injected from JWT)
     * @param cursor    opaque cursor from previous page's nextCursor (null for first page)
     * @param size      number of posts per page (default 20, max 100)
     * @return paginated posts with cursor for next page
     */
    @GetMapping("/following")
    public ResponseEntity<CursorPage<PostDto>> getFollowingFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<PostDto> page = feedService.getFollowingFeed(
                principal.getUserId(),
                cursor,
                size
        );

        return ResponseEntity.ok(page);
    }
}

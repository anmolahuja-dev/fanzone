package com.fanzone.feed.service;

import com.fanzone.common.dto.CursorPage;
import com.fanzone.feed.dto.PostDto;
import com.fanzone.feed.model.PostEntity;
import com.fanzone.feed.model.UserEntity;
import com.fanzone.feed.repository.FeedRepository;
import com.fanzone.feed.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link FeedService} providing cursor-based pagination
 * for For You, Club, and Following feed tabs.
 * <p>
 * Pagination strategy:
 * - Fetch size+1 posts to determine if there are more pages.
 * - The cursor is the last post's ID.
 * - Posts are enriched with author info (username, reputation level, avatar).
 */
@Service
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;

    public FeedServiceImpl(FeedRepository feedRepository, UserRepository userRepository) {
        this.feedRepository = feedRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CursorPage<PostDto> getForYouFeed(UUID userId, UUID userClubId, String cursor, int size) {
        int pageSize = resolvePageSize(size);
        Instant now = Instant.now();

        List<PostEntity> posts = feedRepository.findForYouFeed(userClubId, cursor, pageSize, now);

        return buildCursorPage(posts, pageSize);
    }

    @Override
    public CursorPage<PostDto> getClubFeed(UUID userClubId, String cursor, int size) {
        int pageSize = resolvePageSize(size);

        List<PostEntity> posts = feedRepository.findClubFeed(userClubId, cursor, pageSize);

        return buildCursorPage(posts, pageSize);
    }

    @Override
    public CursorPage<PostDto> getFollowingFeed(UUID userId, String cursor, int size) {
        int pageSize = resolvePageSize(size);

        List<PostEntity> posts = feedRepository.findFollowingFeed(userId, cursor, pageSize);

        return buildCursorPage(posts, pageSize);
    }

    /**
     * Builds a CursorPage from the fetched posts.
     * If we got size+1 results, there are more pages. We trim to size and set the cursor.
     */
    private CursorPage<PostDto> buildCursorPage(List<PostEntity> posts, int pageSize) {
        boolean hasMore = posts.size() > pageSize;
        List<PostEntity> pageItems = hasMore ? posts.subList(0, pageSize) : posts;

        // Enrich posts with author info
        List<PostDto> dtos = enrichWithAuthorInfo(pageItems);

        String nextCursor = hasMore ? pageItems.get(pageItems.size() - 1).getId().toString() : null;

        return new CursorPage<>(dtos, nextCursor, hasMore);
    }

    /**
     * Fetches user info for all post authors and maps PostEntity -> PostDto with enrichment.
     */
    private List<PostDto> enrichWithAuthorInfo(List<PostEntity> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<UUID> authorIds = posts.stream()
                .map(PostEntity::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, UserEntity> userMap = userRepository.findAllByIdIn(authorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return posts.stream()
                .map(post -> toPostDto(post, userMap.get(post.getUserId())))
                .collect(Collectors.toList());
    }

    private PostDto toPostDto(PostEntity post, UserEntity author) {
        return new PostDto(
                post.getId(),
                post.getUserId(),
                author != null ? author.getUsername() : null,
                author != null ? author.getReputationLevel() : null,
                author != null ? author.getProfilePictureUrl() : null,
                post.getClubId(),
                post.getPostType(),
                post.getContent(),
                post.getImageUrl(),
                post.getCommentCount(),
                post.getUpvoteCount(),
                post.getCreatedAt()
        );
    }

    private int resolvePageSize(int size) {
        if (size <= 0 || size > 100) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
}

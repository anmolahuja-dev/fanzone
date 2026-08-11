package com.fanzone.feed.service;

import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.feed.dto.ClubHubResponse;
import com.fanzone.feed.dto.PostDto;
import com.fanzone.feed.model.ClubEntity;
import com.fanzone.feed.model.PostEntity;
import com.fanzone.feed.model.UserEntity;
import com.fanzone.feed.repository.ClubRepository;
import com.fanzone.feed.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Club Hub aggregation service.
 * Returns posts for a club organized by section (news, matchday, discussions, etc.)
 * with 10 most recent posts per section.
 */
@Service
@Transactional(readOnly = true)
public class ClubHubService {

    private static final int POSTS_PER_SECTION = 10;

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public ClubHubService(ClubRepository clubRepository, UserRepository userRepository,
                          EntityManager entityManager) {
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public ClubHubResponse getClubHub(UUID clubId) {
        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("CLUB_NOT_FOUND", "Club not found"));

        long activeMemberCount = countActiveMembers(clubId);

        // Get recent posts by club, sorted by recency, then split into sections by content keywords
        List<PostEntity> recentPosts = getRecentPostsForClub(clubId, 60);

        // Categorize posts into sections (MVP: simple keyword-based categorization)
        List<PostDto> news = filterAndEnrich(recentPosts, "news", POSTS_PER_SECTION);
        List<PostDto> matchday = filterAndEnrich(recentPosts, "matchday", POSTS_PER_SECTION);
        List<PostDto> discussions = filterAndEnrich(recentPosts, "text", POSTS_PER_SECTION);
        List<PostDto> transfers = filterAndEnrich(recentPosts, "transfers", POSTS_PER_SECTION);
        List<PostDto> memes = filterAndEnrich(recentPosts, "memes", POSTS_PER_SECTION);
        List<PostDto> tactical = filterAndEnrich(recentPosts, "match_analysis", POSTS_PER_SECTION);

        return new ClubHubResponse(
                club.getId(),
                club.getName(),
                club.getLogoUrl(),
                activeMemberCount,
                null, // Live match info would come from match-thread-service via REST call
                news,
                matchday,
                discussions,
                transfers,
                memes,
                tactical
        );
    }

    private List<PostEntity> getRecentPostsForClub(UUID clubId, int limit) {
        TypedQuery<PostEntity> query = entityManager.createQuery(
                "SELECT p FROM PostEntity p WHERE p.clubId = :clubId AND p.isFlagged = false " +
                        "ORDER BY p.createdAt DESC", PostEntity.class);
        query.setParameter("clubId", clubId);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    private long countActiveMembers(UUID clubId) {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Long count = entityManager.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE u.favoriteClubId = :clubId",
                Long.class)
                .setParameter("clubId", clubId)
                .getSingleResult();
        return count != null ? count : 0;
    }

    /**
     * Filters posts by type/content keyword and enriches with author info.
     * MVP categorization: match_analysis → tactical, text → discussions, etc.
     */
    private List<PostDto> filterAndEnrich(List<PostEntity> posts, String category, int limit) {
        List<PostEntity> filtered = posts.stream()
                .filter(p -> matchesCategory(p, category))
                .limit(limit)
                .toList();

        return enrichWithAuthorInfo(filtered);
    }

    private boolean matchesCategory(PostEntity post, String category) {
        String postType = post.getPostType();
        String content = post.getContent() != null ? post.getContent().toLowerCase() : "";

        return switch (category) {
            case "match_analysis" -> "match_analysis".equals(postType);
            case "text" -> "text".equals(postType);
            case "news" -> content.contains("news") || content.contains("breaking") || content.contains("official");
            case "matchday" -> content.contains("matchday") || content.contains("lineup") || content.contains("kick off");
            case "transfers" -> content.contains("transfer") || content.contains("signing") || content.contains("rumour");
            case "memes" -> content.contains("meme") || "image".equals(postType);
            default -> false;
        };
    }

    private List<PostDto> enrichWithAuthorInfo(List<PostEntity> posts) {
        if (posts.isEmpty()) return List.of();

        List<UUID> authorIds = posts.stream().map(PostEntity::getUserId).distinct().toList();
        Map<UUID, UserEntity> userMap = userRepository.findAllByIdIn(authorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return posts.stream()
                .map(post -> {
                    UserEntity author = userMap.get(post.getUserId());
                    return new PostDto(
                            post.getId(), post.getUserId(),
                            author != null ? author.getUsername() : null,
                            author != null ? author.getReputationLevel() : null,
                            author != null ? author.getProfilePictureUrl() : null,
                            post.getClubId(), post.getPostType(), post.getContent(),
                            post.getImageUrl(), post.getCommentCount(), post.getUpvoteCount(),
                            post.getCreatedAt()
                    );
                })
                .toList();
    }
}

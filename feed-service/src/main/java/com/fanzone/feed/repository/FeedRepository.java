package com.fanzone.feed.repository;

import com.fanzone.feed.model.PostEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Custom repository for feed queries with cursor-based pagination.
 * Uses EntityManager directly for complex native queries (e.g., For You with compute_feed_score()).
 * <p>
 * Cursor strategy: the cursor is the last post's ID. Next page fetches posts with
 * created_at less than the cursor post's created_at. For For You feed, the cursor
 * encodes the score and created_at for proper ordering.
 */
@Repository
public class FeedRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Fetches posts for the "For You" feed, scored using the PostgreSQL compute_feed_score() function.
     * Orders by score DESC, then created_at DESC.
     * <p>
     * Cursor for "For You" is the post ID. When a cursor is provided, we exclude posts
     * that would appear before the cursor position (using a subquery to get the cursor post's score and time).
     *
     * @param userClubId the user's favorite club ID
     * @param cursor     the last post ID from previous page (null for first page)
     * @param size       number of posts to fetch (fetch size+1 to determine hasMore)
     * @param now        the current timestamp for scoring
     * @return list of posts (up to size+1 elements)
     */
    @SuppressWarnings("unchecked")
    public List<PostEntity> findForYouFeed(UUID userClubId, String cursor, int size, Instant now) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.* FROM posts p ");
        sql.append("JOIN users u ON u.id = p.user_id ");
        sql.append("WHERE p.is_flagged = false ");

        if (cursor != null) {
            // Use cursor to paginate: get posts that rank lower than the cursor post
            sql.append("AND (compute_feed_score(p.club_id, :userClubId, u.reputation_level, p.last_activity_at, p.comment_count, p.toxic_report_count, :now), p.created_at, p.id) < ");
            sql.append("(SELECT compute_feed_score(cp.club_id, :userClubId, cu.reputation_level, cp.last_activity_at, cp.comment_count, cp.toxic_report_count, :now), cp.created_at, cp.id ");
            sql.append("FROM posts cp JOIN users cu ON cu.id = cp.user_id WHERE cp.id = :cursor) ");
        }

        sql.append("ORDER BY compute_feed_score(p.club_id, :userClubId, u.reputation_level, p.last_activity_at, p.comment_count, p.toxic_report_count, :now) DESC, ");
        sql.append("p.created_at DESC, p.id DESC ");
        sql.append("LIMIT :limit");

        var query = entityManager.createNativeQuery(sql.toString(), PostEntity.class);
        query.setParameter("userClubId", userClubId);
        query.setParameter("now", now);
        query.setParameter("limit", size + 1);

        if (cursor != null) {
            query.setParameter("cursor", UUID.fromString(cursor));
        }

        return query.getResultList();
    }

    /**
     * Fetches posts for the "Club" feed tab.
     * Filters by club_id matching user's favorite club, ordered by created_at DESC.
     *
     * @param clubId the club ID to filter by
     * @param cursor the last post ID from previous page (null for first page)
     * @param size   number of posts to fetch (fetch size+1 to determine hasMore)
     * @return list of posts (up to size+1 elements)
     */
    public List<PostEntity> findClubFeed(UUID clubId, String cursor, int size) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM PostEntity p WHERE p.clubId = :clubId AND p.isFlagged = false ");

        if (cursor != null) {
            jpql.append("AND p.createdAt < (SELECT cp.createdAt FROM PostEntity cp WHERE cp.id = :cursor) ");
        }

        jpql.append("ORDER BY p.createdAt DESC, p.id DESC");

        TypedQuery<PostEntity> query = entityManager.createQuery(jpql.toString(), PostEntity.class);
        query.setParameter("clubId", clubId);
        query.setMaxResults(size + 1);

        if (cursor != null) {
            query.setParameter("cursor", UUID.fromString(cursor));
        }

        return query.getResultList();
    }

    /**
     * Fetches posts for the "Following" feed tab.
     * Filters posts authored by users that the current user follows, ordered by created_at DESC.
     *
     * @param userId the current user's ID (to look up who they follow)
     * @param cursor the last post ID from previous page (null for first page)
     * @param size   number of posts to fetch (fetch size+1 to determine hasMore)
     * @return list of posts (up to size+1 elements)
     */
    public List<PostEntity> findFollowingFeed(UUID userId, String cursor, int size) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM PostEntity p WHERE p.userId IN ");
        jpql.append("(SELECT f.followedId FROM FollowEntity f WHERE f.followerId = :userId) ");
        jpql.append("AND p.isFlagged = false ");

        if (cursor != null) {
            jpql.append("AND p.createdAt < (SELECT cp.createdAt FROM PostEntity cp WHERE cp.id = :cursor) ");
        }

        jpql.append("ORDER BY p.createdAt DESC, p.id DESC");

        TypedQuery<PostEntity> query = entityManager.createQuery(jpql.toString(), PostEntity.class);
        query.setParameter("userId", userId);
        query.setMaxResults(size + 1);

        if (cursor != null) {
            query.setParameter("cursor", UUID.fromString(cursor));
        }

        return query.getResultList();
    }
}

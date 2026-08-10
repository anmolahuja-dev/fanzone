package com.fanzone.post.repository;

import com.fanzone.post.model.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    /**
     * Find top-level comments (no parent) for a post, ordered by creation time.
     */
    List<CommentEntity> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(UUID postId);

    /**
     * Find all replies to a specific parent comment.
     */
    List<CommentEntity> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    /**
     * Find all comments for a post (flat list, useful for building trees).
     */
    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(UUID postId);

    @Modifying
    @Query("UPDATE CommentEntity c SET c.upvoteCount = c.upvoteCount + 1 WHERE c.id = :commentId")
    void incrementUpvoteCount(@Param("commentId") UUID commentId);

    @Modifying
    @Query("UPDATE CommentEntity c SET c.upvoteCount = c.upvoteCount - 1 WHERE c.id = :commentId AND c.upvoteCount > 0")
    void decrementUpvoteCount(@Param("commentId") UUID commentId);
}

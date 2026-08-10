package com.fanzone.post.repository;

import com.fanzone.post.model.CommentUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentUpvoteRepository extends JpaRepository<CommentUpvoteEntity, CommentUpvoteEntity.CommentUpvoteId> {

    boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

    void deleteByUserIdAndCommentId(UUID userId, UUID commentId);
}

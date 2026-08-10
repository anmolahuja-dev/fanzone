package com.fanzone.post.repository;

import com.fanzone.post.model.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    @Modifying
    @Query("UPDATE PostEntity p SET p.upvoteCount = p.upvoteCount + 1 WHERE p.id = :postId")
    void incrementUpvoteCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.upvoteCount = p.upvoteCount - 1 WHERE p.id = :postId AND p.upvoteCount > 0")
    void decrementUpvoteCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.toxicReportCount = p.toxicReportCount + 1 WHERE p.id = :postId")
    void incrementToxicReportCount(@Param("postId") UUID postId);
}

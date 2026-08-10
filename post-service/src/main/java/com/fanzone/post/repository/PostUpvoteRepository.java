package com.fanzone.post.repository;

import com.fanzone.post.model.PostUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostUpvoteRepository extends JpaRepository<PostUpvoteEntity, PostUpvoteEntity.PostUpvoteId> {

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);
}

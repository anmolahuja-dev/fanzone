package com.fanzone.post.repository;

import com.fanzone.post.model.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<FollowEntity, FollowEntity.FollowId> {

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.followedId = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.followerId = :userId")
    long countFollowing(@Param("userId") UUID userId);
}

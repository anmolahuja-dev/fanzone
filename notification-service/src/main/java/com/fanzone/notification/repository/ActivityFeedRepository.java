package com.fanzone.notification.repository;

import com.fanzone.notification.model.ActivityFeedEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityFeedRepository extends JpaRepository<ActivityFeedEntity, UUID> {

    List<ActivityFeedEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM ActivityFeedEntity a WHERE a.userId = :userId AND a.isRead = false")
    long countUnread(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE ActivityFeedEntity a SET a.isRead = true WHERE a.userId = :userId AND a.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);
}

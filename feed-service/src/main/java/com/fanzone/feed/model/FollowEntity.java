package com.fanzone.feed.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only JPA entity mapped to the follows table.
 * Used by feed-service to determine which users the current user follows
 * for the "Following" feed tab.
 */
@Entity
@Table(name = "follows")
@Immutable
@IdClass(FollowEntityId.class)
public class FollowEntity {

    @Id
    @Column(name = "follower_id")
    private UUID followerId;

    @Id
    @Column(name = "followed_id")
    private UUID followedId;

    @Column(name = "created_at")
    private Instant createdAt;

    protected FollowEntity() {
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public UUID getFollowedId() {
        return followedId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

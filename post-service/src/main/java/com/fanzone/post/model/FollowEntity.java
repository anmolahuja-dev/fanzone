package com.fanzone.post.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follows")
@IdClass(FollowEntity.FollowId.class)
@Getter
@Setter
public class FollowEntity {

    @Id
    @Column(name = "follower_id")
    private UUID followerId;

    @Id
    @Column(name = "followed_id")
    private UUID followedId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public static class FollowId implements Serializable {
        private UUID followerId;
        private UUID followedId;

        public FollowId() {}

        public FollowId(UUID followerId, UUID followedId) {
            this.followerId = followerId;
            this.followedId = followedId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FollowId that)) return false;
            return followerId.equals(that.followerId) && followedId.equals(that.followedId);
        }

        @Override
        public int hashCode() {
            return 31 * followerId.hashCode() + followedId.hashCode();
        }
    }
}

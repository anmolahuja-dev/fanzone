package com.fanzone.post.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "post_upvotes")
@IdClass(PostUpvoteEntity.PostUpvoteId.class)
@Getter
@Setter
public class PostUpvoteEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "post_id")
    private UUID postId;

    public static class PostUpvoteId implements Serializable {
        private UUID userId;
        private UUID postId;

        public PostUpvoteId() {}

        public PostUpvoteId(UUID userId, UUID postId) {
            this.userId = userId;
            this.postId = postId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostUpvoteId that)) return false;
            return userId.equals(that.userId) && postId.equals(that.postId);
        }

        @Override
        public int hashCode() {
            return 31 * userId.hashCode() + postId.hashCode();
        }
    }
}

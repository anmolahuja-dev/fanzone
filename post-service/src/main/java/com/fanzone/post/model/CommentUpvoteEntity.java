package com.fanzone.post.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "comment_upvotes")
@IdClass(CommentUpvoteEntity.CommentUpvoteId.class)
@Getter
@Setter
public class CommentUpvoteEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "comment_id")
    private UUID commentId;

    public static class CommentUpvoteId implements Serializable {
        private UUID userId;
        private UUID commentId;

        public CommentUpvoteId() {}

        public CommentUpvoteId(UUID userId, UUID commentId) {
            this.userId = userId;
            this.commentId = commentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CommentUpvoteId that)) return false;
            return userId.equals(that.userId) && commentId.equals(that.commentId);
        }

        @Override
        public int hashCode() {
            return 31 * userId.hashCode() + commentId.hashCode();
        }
    }
}

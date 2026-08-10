package com.fanzone.post.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "parent_comment_id")
    private UUID parentCommentId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "upvote_count")
    private int upvoteCount = 0;

    @Column(name = "nesting_level", nullable = false)
    private int nestingLevel = 0;

    @Column(name = "is_flagged")
    private boolean isFlagged = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

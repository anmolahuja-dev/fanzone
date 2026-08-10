package com.fanzone.feed.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only JPA entity mapped to the posts table.
 * Feed-service does not write to this table — it only reads for feed assembly.
 */
@Entity
@Table(name = "posts")
@Immutable
public class PostEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "post_type", nullable = false)
    private String postType;

    @Column(name = "content")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "comment_count")
    private int commentCount;

    @Column(name = "upvote_count")
    private int upvoteCount;

    @Column(name = "toxic_report_count")
    private int toxicReportCount;

    @Column(name = "is_flagged")
    private boolean isFlagged;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "created_at")
    private Instant createdAt;

    protected PostEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getClubId() {
        return clubId;
    }

    public String getPostType() {
        return postType;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public int getToxicReportCount() {
        return toxicReportCount;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.fanzone.post.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private int commentCount = 0;

    @Column(name = "upvote_count")
    private int upvoteCount = 0;

    @Column(name = "toxic_report_count")
    private int toxicReportCount = 0;

    @Column(name = "is_flagged")
    private boolean isFlagged = false;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PollOptionEntity> pollOptions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (lastActivityAt == null) {
            lastActivityAt = Instant.now();
        }
    }
}

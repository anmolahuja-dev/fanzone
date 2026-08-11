package com.fanzone.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_feed")
@Getter
@Setter
public class ActivityFeedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "preview")
    private String preview;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "is_read")
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

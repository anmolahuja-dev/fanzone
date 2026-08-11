package com.fanzone.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityFeedResponse(
        UUID id,
        String activityType,
        String title,
        String preview,
        UUID referenceId,
        boolean isRead,
        Instant createdAt
) {}

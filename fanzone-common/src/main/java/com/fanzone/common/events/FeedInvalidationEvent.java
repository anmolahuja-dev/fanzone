package com.fanzone.common.events;

import java.time.Instant;
import java.util.UUID;

public record FeedInvalidationEvent(
        UUID clubId,
        UUID postId,
        String reason,
        Instant timestamp
) {}

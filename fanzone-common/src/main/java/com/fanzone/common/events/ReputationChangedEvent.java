package com.fanzone.common.events;

import java.time.Instant;
import java.util.UUID;

public record ReputationChangedEvent(
        UUID userId,
        int previousScore,
        int newScore,
        String previousLevel,
        String newLevel,
        String eventType,
        Instant timestamp
) {}

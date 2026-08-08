package com.fanzone.common.events;

import java.time.Instant;
import java.util.UUID;

public record MatchStatusChangedEvent(
        UUID matchId,
        String previousStatus,
        String newStatus,
        Instant timestamp
) {}

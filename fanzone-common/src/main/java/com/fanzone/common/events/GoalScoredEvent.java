package com.fanzone.common.events;

import java.time.Instant;
import java.util.UUID;

public record GoalScoredEvent(
        UUID matchId,
        UUID homeClubId,
        UUID awayClubId,
        String scorerName,
        int homeScore,
        int awayScore,
        int minute,
        Instant timestamp
) {}

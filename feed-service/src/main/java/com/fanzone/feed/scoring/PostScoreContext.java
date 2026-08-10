package com.fanzone.feed.scoring;

import com.fanzone.common.enums.ReputationLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable input DTO for feed scoring.
 * Decouples the scorer from JPA entities by carrying only the fields needed for score computation.
 */
public record PostScoreContext(
        UUID postClubId,
        ReputationLevel authorLevel,
        Instant lastActivityAt,
        int commentCount,
        int toxicReportCount
) {}

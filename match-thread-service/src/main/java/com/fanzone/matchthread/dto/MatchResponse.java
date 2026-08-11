package com.fanzone.matchthread.dto;

import com.fanzone.common.enums.MatchPhase;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID homeClubId,
        UUID awayClubId,
        int homeScore,
        int awayScore,
        MatchPhase status,
        Instant kickOffTime,
        Instant finishedAt
) {}

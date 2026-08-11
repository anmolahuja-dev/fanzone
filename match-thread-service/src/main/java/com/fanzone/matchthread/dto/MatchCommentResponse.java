package com.fanzone.matchthread.dto;

import java.time.Instant;
import java.util.UUID;

public record MatchCommentResponse(
        UUID id,
        UUID matchId,
        UUID userId,
        String content,
        Instant createdAt
) {}

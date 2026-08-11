package com.fanzone.matchthread.dto;

import java.util.UUID;

public record MotmResponse(
        UUID playerId,
        double averageRating,
        long totalRatings,
        int rank
) {}

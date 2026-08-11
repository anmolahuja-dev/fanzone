package com.fanzone.matchthread.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlayerRatingRequest(
        @NotNull(message = "Player ID is required")
        UUID playerId,

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 10, message = "Rating must not exceed 10")
        int rating
) {}

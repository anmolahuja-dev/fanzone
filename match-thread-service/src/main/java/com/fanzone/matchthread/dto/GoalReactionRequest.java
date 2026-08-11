package com.fanzone.matchthread.dto;

import jakarta.validation.constraints.NotBlank;

public record GoalReactionRequest(
        @NotBlank(message = "Reaction type is required")
        String reactionType
) {}

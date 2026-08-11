package com.fanzone.moderation.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerationRequest(
        @NotBlank(message = "Content is required")
        String content
) {}

package com.fanzone.matchthread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatchCommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 500, message = "Comment must not exceed 500 characters")
        String content
) {}

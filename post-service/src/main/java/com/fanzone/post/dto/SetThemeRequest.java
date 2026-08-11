package com.fanzone.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SetThemeRequest(
        @NotNull(message = "Theme preference is required")
        @Pattern(regexp = "light|dark|system", message = "Theme must be 'light', 'dark', or 'system'")
        String theme
) {}

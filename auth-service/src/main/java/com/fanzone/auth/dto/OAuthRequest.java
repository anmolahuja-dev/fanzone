package com.fanzone.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthRequest(
        @NotBlank(message = "ID token is required")
        String idToken
) {
}

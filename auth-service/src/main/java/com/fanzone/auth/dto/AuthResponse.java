package com.fanzone.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String username,
        String email,
        String accessToken,
        String refreshToken,
        boolean emailVerified
) {
}

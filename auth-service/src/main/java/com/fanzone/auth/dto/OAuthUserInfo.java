package com.fanzone.auth.dto;

public record OAuthUserInfo(
        String email,
        String displayName,
        String provider
) {
}

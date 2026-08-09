package com.fanzone.auth.service;

import com.fanzone.auth.dto.AuthResponse;
import com.fanzone.auth.dto.LoginRequest;
import com.fanzone.auth.dto.RegisterRequest;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse oauthLogin(String provider, String token);

    AuthResponse refreshToken(String refreshToken);

    void logout(UUID userId);
}

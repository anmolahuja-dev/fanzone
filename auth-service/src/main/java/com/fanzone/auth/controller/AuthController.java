package com.fanzone.auth.controller;

import com.fanzone.auth.dto.AuthResponse;
import com.fanzone.auth.dto.LoginRequest;
import com.fanzone.auth.dto.OAuthRequest;
import com.fanzone.auth.dto.RefreshTokenRequest;
import com.fanzone.auth.dto.RegisterRequest;
import com.fanzone.auth.service.AuthService;
import com.fanzone.auth.service.EmailVerificationService;
import com.fanzone.auth.service.OAuthService;
import com.fanzone.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService,
                          OAuthService oAuthService,
                          EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.oAuthService = oAuthService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody OAuthRequest request) {
        AuthResponse response = oAuthService.authenticate("google", request.idToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth/apple")
    public ResponseEntity<AuthResponse> appleAuth(@Valid @RequestBody OAuthRequest request) {
        AuthResponse response = oAuthService.authenticate("apple", request.idToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@AuthenticationPrincipal UserPrincipal principal) {
        emailVerificationService.resendVerificationEmail(principal.getUserId());
        return ResponseEntity.ok().build();
    }
}

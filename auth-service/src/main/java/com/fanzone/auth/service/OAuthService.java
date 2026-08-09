package com.fanzone.auth.service;

import com.fanzone.auth.dto.AuthResponse;
import com.fanzone.auth.dto.OAuthUserInfo;
import com.fanzone.auth.model.UserEntity;
import com.fanzone.auth.oauth.AppleOAuthClient;
import com.fanzone.auth.oauth.GoogleOAuthClient;
import com.fanzone.auth.repository.UserRepository;
import com.fanzone.common.exceptions.AuthenticationException;
import com.fanzone.common.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final GoogleOAuthClient googleOAuthClient;
    private final AppleOAuthClient appleOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final Random random = new Random();

    public OAuthService(GoogleOAuthClient googleOAuthClient,
                        AppleOAuthClient appleOAuthClient,
                        UserRepository userRepository,
                        JwtTokenProvider jwtTokenProvider) {
        this.googleOAuthClient = googleOAuthClient;
        this.appleOAuthClient = appleOAuthClient;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse authenticate(String provider, String idToken) {
        OAuthUserInfo userInfo = verifyTokenWithProvider(provider, idToken);

        Optional<UserEntity> existingUser = userRepository.findByEmail(userInfo.email());

        if (existingUser.isPresent()) {
            return loginExistingUser(existingUser.get());
        } else {
            return createNewUser(userInfo);
        }
    }

    private OAuthUserInfo verifyTokenWithProvider(String provider, String idToken) {
        return switch (provider.toLowerCase()) {
            case "google" -> googleOAuthClient.verifyToken(idToken);
            case "apple" -> appleOAuthClient.verifyToken(idToken);
            default -> throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
        };
    }

    private AuthResponse loginExistingUser(UserEntity user) {
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getUsername(), user.getFavoriteClubId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessToken,
                refreshToken,
                user.getEmailVerified()
        );
    }

    private AuthResponse createNewUser(OAuthUserInfo userInfo) {
        String username = generateUniqueUsername(userInfo.email());

        UserEntity newUser = UserEntity.builder()
                .email(userInfo.email())
                .username(username)
                .authProvider(userInfo.provider())
                .passwordHash(null)
                .emailVerified(true)
                .lastActiveAt(Instant.now())
                .build();

        UserEntity savedUser = userRepository.save(newUser);

        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(), savedUser.getEmail(), savedUser.getUsername(), savedUser.getFavoriteClubId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());

        log.info("Created new OAuth user: {} (provider: {})", savedUser.getEmail(), userInfo.provider());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                accessToken,
                refreshToken,
                savedUser.getEmailVerified()
        );
    }

    private String generateUniqueUsername(String email) {
        String prefix = email.split("@")[0];
        // Sanitize: keep only alphanumeric and underscores, truncate to 42 chars to leave room for digits
        prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "");
        if (prefix.length() > 42) {
            prefix = prefix.substring(0, 42);
        }
        if (prefix.isBlank()) {
            prefix = "user";
        }

        if (!userRepository.existsByUsername(prefix)) {
            return prefix;
        }

        // Add random 4 digits to resolve conflicts
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = prefix + String.format("%04d", random.nextInt(10000));
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        // Fallback: use timestamp-based suffix
        return prefix + System.currentTimeMillis() % 100000;
    }
}

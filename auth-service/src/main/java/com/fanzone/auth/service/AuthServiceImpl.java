package com.fanzone.auth.service;

import com.fanzone.auth.dto.AuthResponse;
import com.fanzone.auth.dto.LoginRequest;
import com.fanzone.auth.dto.RegisterRequest;
import com.fanzone.auth.model.UserEntity;
import com.fanzone.auth.repository.UserRepository;
import com.fanzone.auth.validation.EmailValidator;
import com.fanzone.auth.validation.PasswordValidator;
import com.fanzone.common.exceptions.AuthenticationException;
import com.fanzone.common.exceptions.ConflictException;
import com.fanzone.common.exceptions.ValidationException;
import com.fanzone.common.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final Duration SESSION_MAX_INACTIVITY = Duration.ofDays(7);
    private static final String GENERIC_AUTH_ERROR = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Validate email format
        if (!EmailValidator.isValid(request.email())) {
            throw new ValidationException("AUTH_VALIDATION_FAILED",
                    "Invalid email format");
        }

        // Validate password strength
        PasswordValidator.ValidationResult passwordResult = PasswordValidator.validate(request.password());
        if (!passwordResult.isValid()) {
            throw new ValidationException("AUTH_VALIDATION_FAILED", passwordResult.reason());
        }

        // Check email uniqueness
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("AUTH_EMAIL_EXISTS",
                    "An account with this email already exists");
        }

        // Check username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("AUTH_USERNAME_EXISTS",
                    "An account with this username already exists");
        }

        // Create user
        UserEntity user = UserEntity.builder()
                .email(request.email())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authProvider("email")
                .lastActiveAt(Instant.now())
                .build();

        user = userRepository.save(user);

        // Create email verification token and send verification email
        emailVerificationService.createVerificationToken(user.getId());

        // Generate tokens
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Find user by email — use generic error if not found
        Optional<UserEntity> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isEmpty()) {
            // Never reveal that the email doesn't exist
            throw new AuthenticationException("AUTH_INVALID_CREDENTIALS", GENERIC_AUTH_ERROR);
        }

        UserEntity user = optionalUser.get();

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new AuthenticationException("AUTH_ACCOUNT_LOCKED",
                    "Account is temporarily locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            // Always use generic message
            throw new AuthenticationException("AUTH_INVALID_CREDENTIALS", GENERIC_AUTH_ERROR);
        }

        // Successful login — reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse oauthLogin(String provider, String token) {
        // Placeholder — OAuth implementations will call this after verifying the external token
        throw new UnsupportedOperationException("OAuth login not yet implemented for provider: " + provider);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        // Validate the refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("AUTH_INVALID_TOKEN", "Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.extractUserId(refreshToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("AUTH_INVALID_TOKEN",
                        "Invalid refresh token"));

        // Check session validity — lastActiveAt must be within 7 days
        if (isSessionExpired(user.getLastActiveAt())) {
            throw new AuthenticationException("AUTH_SESSION_EXPIRED",
                    "Session has expired due to inactivity. Please log in again.");
        }

        // Generate new access token (refresh token remains the same)
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getUsername(), user.getFavoriteClubId(),
                Boolean.TRUE.equals(user.getEmailVerified()));

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessToken,
                refreshToken,
                user.getEmailVerified()
        );
    }

    @Override
    public void logout(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(Instant.now());
            userRepository.save(user);
        });
    }

    /**
     * Handles a failed login attempt by incrementing the counter and locking
     * the account after 5 consecutive failures.
     */
    private void handleFailedLogin(UserEntity user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
        }

        userRepository.save(user);
    }

    /**
     * Checks if the session has expired based on the last activity timestamp.
     * A session expires if lastActiveAt is more than 7 days ago.
     */
    private boolean isSessionExpired(Instant lastActiveAt) {
        if (lastActiveAt == null) {
            return true;
        }
        return lastActiveAt.isBefore(Instant.now().minus(SESSION_MAX_INACTIVITY));
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getUsername(), user.getFavoriteClubId(),
                Boolean.TRUE.equals(user.getEmailVerified()));
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
}

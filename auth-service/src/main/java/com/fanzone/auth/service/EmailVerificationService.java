package com.fanzone.auth.service;

import com.fanzone.auth.model.EmailVerificationToken;
import com.fanzone.auth.model.UserEntity;
import com.fanzone.auth.repository.EmailVerificationTokenRepository;
import com.fanzone.auth.repository.UserRepository;
import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.common.exceptions.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class EmailVerificationService {

    private static final Duration TOKEN_EXPIRY = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Creates a new verification token for the given user, saves it to the database,
     * and sends a verification email.
     *
     * @param userId the user ID to create a token for
     * @return the generated token string
     */
    public String createVerificationToken(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("AUTH_USER_NOT_FOUND", "User not found"));

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY))
                .build();

        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);

        return token;
    }

    /**
     * Verifies the email by validating the token, marking the user as email_verified,
     * and deleting the token.
     *
     * @param token the verification token string
     */
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("AUTH_INVALID_TOKEN",
                        "Invalid verification token"));

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(verificationToken);
            throw new ValidationException("AUTH_TOKEN_EXPIRED",
                    "Verification token has expired");
        }

        UserEntity user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new NotFoundException("AUTH_USER_NOT_FOUND", "User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
    }

    /**
     * Resends a verification email by deleting any old token and creating a new one.
     *
     * @param userId the user ID to resend verification for
     */
    public void resendVerificationEmail(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("AUTH_USER_NOT_FOUND", "User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ValidationException("AUTH_ALREADY_VERIFIED",
                    "Email is already verified");
        }

        // Delete old tokens for this user
        tokenRepository.deleteByUserId(userId);

        // Create a new token and send email
        createVerificationToken(userId);
    }
}

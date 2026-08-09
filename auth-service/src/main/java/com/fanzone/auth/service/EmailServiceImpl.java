package com.fanzone.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Placeholder email service that logs the verification link.
 * Replace with a real SMTP implementation for production.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendVerificationEmail(String email, String token) {
        log.info("Verification link: /api/v1/auth/verify-email?token={}", token);
        log.info("Sending verification email to: {}", email);
    }
}

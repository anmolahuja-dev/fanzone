package com.fanzone.auth.service;

/**
 * Service for sending emails. In MVP this is a placeholder that logs the
 * verification link. A real SMTP implementation will replace this in production.
 */
public interface EmailService {

    /**
     * Send a verification email containing the token link.
     *
     * @param email recipient email address
     * @param token the verification token
     */
    void sendVerificationEmail(String email, String token);
}

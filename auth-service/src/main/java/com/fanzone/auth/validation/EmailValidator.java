package com.fanzone.auth.validation;

/**
 * Email validation using simple string operations.
 * Rule: exactly one '@' followed by a domain with at least one dot.
 */
public final class EmailValidator {

    private EmailValidator() {
        // Utility class
    }

    /**
     * Validates that the email contains exactly one '@' symbol
     * and the domain part (after '@') contains at least one dot.
     *
     * @param email the email string to validate
     * @return true if email format is valid
     */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        // Find the '@' symbol
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            return false;
        }

        // Ensure exactly one '@' — no second '@' after the first
        if (email.indexOf('@', atIndex + 1) != -1) {
            return false;
        }

        // Local part (before '@') must not be empty
        String localPart = email.substring(0, atIndex);
        if (localPart.isEmpty()) {
            return false;
        }

        // Domain part (after '@') must contain at least one dot
        String domainPart = email.substring(atIndex + 1);
        if (domainPart.isEmpty()) {
            return false;
        }

        int dotIndex = domainPart.indexOf('.');
        if (dotIndex == -1) {
            return false;
        }

        // Dot cannot be at start or end of domain
        if (dotIndex == 0 || dotIndex == domainPart.length() - 1) {
            return false;
        }

        return true;
    }
}

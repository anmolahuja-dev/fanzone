package com.fanzone.auth.validation;

/**
 * Password strength validation.
 * Rules: 8-128 characters, at least 1 uppercase, 1 lowercase, 1 digit.
 */
public final class PasswordValidator {

    private PasswordValidator() {
        // Utility class
    }

    /**
     * Validates password strength and returns a result with a specific failure reason.
     *
     * @param password the password to validate
     * @return validation result indicating success or failure with reason
     */
    public static ValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.failure("Password is required");
        }

        if (password.length() < 8) {
            return ValidationResult.failure("Password must be at least 8 characters");
        }

        if (password.length() > 128) {
            return ValidationResult.failure("Password must not exceed 128 characters");
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        if (!hasUppercase) {
            return ValidationResult.failure("Password must contain at least one uppercase letter");
        }

        if (!hasLowercase) {
            return ValidationResult.failure("Password must contain at least one lowercase letter");
        }

        if (!hasDigit) {
            return ValidationResult.failure("Password must contain at least one digit");
        }

        return ValidationResult.success();
    }

    /**
     * Convenience method that returns true if the password meets all requirements.
     */
    public static boolean isValid(String password) {
        return validate(password).isValid();
    }

    /**
     * Result of password validation containing validity flag and optional failure reason.
     */
    public record ValidationResult(boolean isValid, String reason) {

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}

package com.fanzone.post.validation;

import com.fanzone.common.enums.PostType;
import com.fanzone.common.exceptions.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * Validates post content based on post type.
 * <p>
 * Validation rules:
 * <ul>
 *   <li>TEXT: content 1-2000 characters</li>
 *   <li>IMAGE: requires image file (JPEG/PNG/WebP, max 10MB), optional text 0-2000 chars</li>
 *   <li>POLL: question (content) 1-200 chars, 2-4 options each 1-100 chars</li>
 *   <li>MATCH_ANALYSIS: content 1-10000 characters</li>
 * </ul>
 * A post with no content and no image is always rejected.
 */
public final class PostValidator {

    private PostValidator() {
        // Utility class
    }

    private static final int TEXT_MAX_LENGTH = 2000;
    private static final int POLL_QUESTION_MAX_LENGTH = 200;
    private static final int POLL_OPTION_MAX_LENGTH = 100;
    private static final int POLL_MIN_OPTIONS = 2;
    private static final int POLL_MAX_OPTIONS = 4;
    private static final int MATCH_ANALYSIS_MAX_LENGTH = 10000;
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /**
     * Validates a text post.
     *
     * @param content the post content
     * @throws ValidationException if validation fails
     */
    public static void validateTextPost(String content) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("POST_CONTENT_REQUIRED", "Text post requires content");
        }
        if (content.length() > TEXT_MAX_LENGTH) {
            throw new ValidationException("POST_CONTENT_TOO_LONG",
                    "Text post content must not exceed " + TEXT_MAX_LENGTH + " characters");
        }
    }

    /**
     * Validates a poll post.
     *
     * @param question the poll question (stored as content)
     * @param options  the poll options
     * @throws ValidationException if validation fails
     */
    public static void validatePollPost(String question, List<String> options) {
        if (question == null || question.isBlank()) {
            throw new ValidationException("POLL_QUESTION_REQUIRED", "Poll requires a question");
        }
        if (question.length() > POLL_QUESTION_MAX_LENGTH) {
            throw new ValidationException("POLL_QUESTION_TOO_LONG",
                    "Poll question must not exceed " + POLL_QUESTION_MAX_LENGTH + " characters");
        }

        if (options == null || options.size() < POLL_MIN_OPTIONS) {
            throw new ValidationException("POLL_OPTIONS_MIN",
                    "Poll requires at least " + POLL_MIN_OPTIONS + " options");
        }
        if (options.size() > POLL_MAX_OPTIONS) {
            throw new ValidationException("POLL_OPTIONS_MAX",
                    "Poll must not have more than " + POLL_MAX_OPTIONS + " options");
        }

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option == null || option.isBlank()) {
                throw new ValidationException("POLL_OPTION_EMPTY",
                        "Poll option " + (i + 1) + " must not be empty");
            }
            if (option.length() > POLL_OPTION_MAX_LENGTH) {
                throw new ValidationException("POLL_OPTION_TOO_LONG",
                        "Poll option " + (i + 1) + " must not exceed " + POLL_OPTION_MAX_LENGTH + " characters");
            }
        }
    }

    /**
     * Validates a match analysis post.
     *
     * @param content the analysis content
     * @throws ValidationException if validation fails
     */
    public static void validateMatchAnalysis(String content) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("ANALYSIS_CONTENT_REQUIRED",
                    "Match analysis requires content");
        }
        if (content.length() > MATCH_ANALYSIS_MAX_LENGTH) {
            throw new ValidationException("ANALYSIS_CONTENT_TOO_LONG",
                    "Match analysis must not exceed " + MATCH_ANALYSIS_MAX_LENGTH + " characters");
        }
    }

    /**
     * Validates an image file (format and size).
     *
     * @param file the uploaded image file
     * @throws ValidationException if validation fails
     */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("IMAGE_REQUIRED", "Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("IMAGE_FORMAT_INVALID",
                    "Image must be JPEG, PNG, or WebP format");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ValidationException("IMAGE_TOO_LARGE",
                    "Image must not exceed 10MB");
        }
    }

    /**
     * Validates an image post (image required, optional text).
     *
     * @param content optional text content (may be null or blank)
     * @param file    the uploaded image file
     * @throws ValidationException if validation fails
     */
    public static void validateImagePost(String content, MultipartFile file) {
        validateImage(file);

        // Optional text content — if present, enforce limit
        if (content != null && !content.isBlank() && content.length() > TEXT_MAX_LENGTH) {
            throw new ValidationException("POST_CONTENT_TOO_LONG",
                    "Image post text must not exceed " + TEXT_MAX_LENGTH + " characters");
        }
    }

    /**
     * Validates a post based on its type.
     * Convenience method that dispatches to the appropriate type-specific validator.
     *
     * @param postType the type of post
     * @param content  the post content (text, question, or analysis)
     * @param options  poll options (only used for POLL type, null for others)
     * @param image    image file (only used for IMAGE type, null for others)
     * @throws ValidationException if validation fails
     */
    public static void validate(PostType postType, String content, List<String> options, MultipartFile image) {
        switch (postType) {
            case TEXT -> validateTextPost(content);
            case IMAGE -> validateImagePost(content, image);
            case POLL -> validatePollPost(content, options);
            case MATCH_ANALYSIS -> validateMatchAnalysis(content);
        }
    }

    // --- Pure validation methods for property-based testing (no MultipartFile) ---

    /**
     * Validates text content length for a given post type.
     * Returns a ValidationResult suitable for property-based testing.
     *
     * @param postType the type of post
     * @param content  the content string
     * @return validation result
     */
    public static ValidationResult validateContent(PostType postType, String content) {
        return switch (postType) {
            case TEXT -> validateTextContent(content);
            case MATCH_ANALYSIS -> validateAnalysisContent(content);
            case POLL -> validatePollQuestion(content);
            case IMAGE -> validateImageContent(content);
        };
    }

    private static ValidationResult validateTextContent(String content) {
        if (content == null || content.isBlank()) {
            return ValidationResult.failure("Text post requires content");
        }
        if (content.length() > TEXT_MAX_LENGTH) {
            return ValidationResult.failure("Text post content exceeds " + TEXT_MAX_LENGTH + " characters");
        }
        return ValidationResult.success();
    }

    private static ValidationResult validateAnalysisContent(String content) {
        if (content == null || content.isBlank()) {
            return ValidationResult.failure("Match analysis requires content");
        }
        if (content.length() > MATCH_ANALYSIS_MAX_LENGTH) {
            return ValidationResult.failure("Match analysis exceeds " + MATCH_ANALYSIS_MAX_LENGTH + " characters");
        }
        return ValidationResult.success();
    }

    private static ValidationResult validatePollQuestion(String question) {
        if (question == null || question.isBlank()) {
            return ValidationResult.failure("Poll requires a question");
        }
        if (question.length() > POLL_QUESTION_MAX_LENGTH) {
            return ValidationResult.failure("Poll question exceeds " + POLL_QUESTION_MAX_LENGTH + " characters");
        }
        return ValidationResult.success();
    }

    private static ValidationResult validateImageContent(String content) {
        // For image posts, content is optional
        if (content != null && !content.isBlank() && content.length() > TEXT_MAX_LENGTH) {
            return ValidationResult.failure("Image post text exceeds " + TEXT_MAX_LENGTH + " characters");
        }
        return ValidationResult.success();
    }

    /**
     * Validates poll options count and individual lengths.
     *
     * @param options the list of option strings
     * @return validation result
     */
    public static ValidationResult validatePollOptions(List<String> options) {
        if (options == null || options.size() < POLL_MIN_OPTIONS) {
            return ValidationResult.failure("Poll requires at least " + POLL_MIN_OPTIONS + " options");
        }
        if (options.size() > POLL_MAX_OPTIONS) {
            return ValidationResult.failure("Poll must not have more than " + POLL_MAX_OPTIONS + " options");
        }
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option == null || option.isBlank()) {
                return ValidationResult.failure("Poll option " + (i + 1) + " is empty");
            }
            if (option.length() > POLL_OPTION_MAX_LENGTH) {
                return ValidationResult.failure("Poll option " + (i + 1) + " exceeds " + POLL_OPTION_MAX_LENGTH + " characters");
            }
        }
        return ValidationResult.success();
    }

    /**
     * Validates image metadata (content type and size) without requiring a MultipartFile.
     * Useful for property-based testing.
     *
     * @param contentType the MIME type of the image
     * @param sizeBytes   the size in bytes
     * @return validation result
     */
    public static ValidationResult validateImageMetadata(String contentType, long sizeBytes) {
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ValidationResult.failure("Image must be JPEG, PNG, or WebP format");
        }
        if (sizeBytes > MAX_IMAGE_SIZE_BYTES) {
            return ValidationResult.failure("Image must not exceed 10MB");
        }
        if (sizeBytes <= 0) {
            return ValidationResult.failure("Image file is empty");
        }
        return ValidationResult.success();
    }

    /**
     * Result of post validation containing validity flag and optional failure reason.
     */
    public record ValidationResult(boolean isValid, String reason) {

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }
    }

    // --- Constants exposed for testing ---

    public static int getTextMaxLength() {
        return TEXT_MAX_LENGTH;
    }

    public static int getPollQuestionMaxLength() {
        return POLL_QUESTION_MAX_LENGTH;
    }

    public static int getPollOptionMaxLength() {
        return POLL_OPTION_MAX_LENGTH;
    }

    public static int getPollMinOptions() {
        return POLL_MIN_OPTIONS;
    }

    public static int getPollMaxOptions() {
        return POLL_MAX_OPTIONS;
    }

    public static int getMatchAnalysisMaxLength() {
        return MATCH_ANALYSIS_MAX_LENGTH;
    }

    public static long getMaxImageSizeBytes() {
        return MAX_IMAGE_SIZE_BYTES;
    }

    public static Set<String> getAllowedImageContentTypes() {
        return ALLOWED_IMAGE_CONTENT_TYPES;
    }
}

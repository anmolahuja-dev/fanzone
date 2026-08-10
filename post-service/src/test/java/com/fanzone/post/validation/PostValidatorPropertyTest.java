package com.fanzone.post.validation;

import com.fanzone.common.enums.PostType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-based tests for PostValidator (Property 5).
 * <p>
 * Validates that:
 * - Text posts are accepted iff content length is 1-2000
 * - Poll questions are accepted iff length is 1-200
 * - Poll options are accepted iff count is 2-4 and each option is 1-100 chars
 * - Match analysis is accepted iff content length is 1-10000
 * - Image metadata is accepted iff content type is JPEG/PNG/WebP and size <= 10MB
 */
class PostValidatorPropertyTest {

    // =================================================================
    // TEXT POST CONTENT VALIDATION
    // =================================================================

    @Property(tries = 200)
    void textPost_validContent_accepted(
            @ForAll @StringLength(min = 1, max = 2000) String content) {
        // Non-blank content within bounds should be valid
        if (!content.isBlank()) {
            PostValidator.ValidationResult result = PostValidator.validateContent(PostType.TEXT, content);
            assert result.isValid() : "Expected valid for text content of length " + content.length();
        }
    }

    @Property(tries = 100)
    void textPost_tooLong_rejected(
            @ForAll @StringLength(min = 2001, max = 5000) String content) {
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.TEXT, content);
        assert !result.isValid() : "Expected invalid for text content of length " + content.length();
    }

    @Property(tries = 50)
    void textPost_emptyOrNull_rejected(@ForAll("emptyOrBlankStrings") String content) {
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.TEXT, content);
        assert !result.isValid() : "Expected invalid for empty/null text content";
    }

    // =================================================================
    // MATCH ANALYSIS CONTENT VALIDATION
    // =================================================================

    @Property(tries = 200)
    void matchAnalysis_validContent_accepted(
            @ForAll @StringLength(min = 1, max = 10000) String content) {
        if (!content.isBlank()) {
            PostValidator.ValidationResult result = PostValidator.validateContent(PostType.MATCH_ANALYSIS, content);
            assert result.isValid() : "Expected valid for analysis of length " + content.length();
        }
    }

    @Property(tries = 100)
    void matchAnalysis_tooLong_rejected(
            @ForAll @StringLength(min = 10001, max = 15000) String content) {
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.MATCH_ANALYSIS, content);
        assert !result.isValid() : "Expected invalid for analysis of length " + content.length();
    }

    @Property(tries = 50)
    void matchAnalysis_emptyOrNull_rejected(@ForAll("emptyOrBlankStrings") String content) {
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.MATCH_ANALYSIS, content);
        assert !result.isValid() : "Expected invalid for empty/null analysis content";
    }

    // =================================================================
    // POLL QUESTION VALIDATION
    // =================================================================

    @Property(tries = 200)
    void pollQuestion_validLength_accepted(
            @ForAll @StringLength(min = 1, max = 200) String question) {
        if (!question.isBlank()) {
            PostValidator.ValidationResult result = PostValidator.validateContent(PostType.POLL, question);
            assert result.isValid() : "Expected valid for poll question of length " + question.length();
        }
    }

    @Property(tries = 100)
    void pollQuestion_tooLong_rejected(
            @ForAll @StringLength(min = 201, max = 500) String question) {
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.POLL, question);
        assert !result.isValid() : "Expected invalid for poll question of length " + question.length();
    }

    // =================================================================
    // POLL OPTIONS VALIDATION
    // =================================================================

    @Property(tries = 200)
    void pollOptions_validCount_accepted(@ForAll("validPollOptions") List<String> options) {
        PostValidator.ValidationResult result = PostValidator.validatePollOptions(options);
        assert result.isValid() : "Expected valid for " + options.size() + " options";
    }

    @Property(tries = 100)
    void pollOptions_tooFew_rejected(@ForAll("tooFewOptions") List<String> options) {
        PostValidator.ValidationResult result = PostValidator.validatePollOptions(options);
        assert !result.isValid() : "Expected invalid for " + (options == null ? "null" : options.size()) + " options";
    }

    @Property(tries = 100)
    void pollOptions_tooMany_rejected(@ForAll("tooManyOptions") List<String> options) {
        PostValidator.ValidationResult result = PostValidator.validatePollOptions(options);
        assert !result.isValid() : "Expected invalid for " + options.size() + " options";
    }

    @Property(tries = 100)
    void pollOptions_optionTooLong_rejected(@ForAll("optionsWithOneTooLong") List<String> options) {
        PostValidator.ValidationResult result = PostValidator.validatePollOptions(options);
        assert !result.isValid() : "Expected invalid for options with one exceeding 100 chars";
    }

    // =================================================================
    // IMAGE METADATA VALIDATION
    // =================================================================

    @Property(tries = 200)
    void imageMetadata_validJpeg_accepted(@ForAll @IntRange(min = 1, max = 10485760) int size) {
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata("image/jpeg", size);
        assert result.isValid() : "Expected valid for JPEG of size " + size;
    }

    @Property(tries = 200)
    void imageMetadata_validPng_accepted(@ForAll @IntRange(min = 1, max = 10485760) int size) {
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata("image/png", size);
        assert result.isValid() : "Expected valid for PNG of size " + size;
    }

    @Property(tries = 200)
    void imageMetadata_validWebp_accepted(@ForAll @IntRange(min = 1, max = 10485760) int size) {
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata("image/webp", size);
        assert result.isValid() : "Expected valid for WebP of size " + size;
    }

    @Property(tries = 100)
    void imageMetadata_tooLarge_rejected(@ForAll("validImageContentType") String contentType) {
        long tooBig = PostValidator.getMaxImageSizeBytes() + 1;
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata(contentType, tooBig);
        assert !result.isValid() : "Expected invalid for image exceeding 10MB";
    }

    @Property(tries = 100)
    void imageMetadata_invalidContentType_rejected(@ForAll("invalidImageContentType") String contentType) {
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata(contentType, 1024);
        assert !result.isValid() : "Expected invalid for content type: " + contentType;
    }

    // =================================================================
    // BOUNDARY TESTS — content at exact limits
    // =================================================================

    @Example
    void textPost_exactlyAtLimit_accepted() {
        String content = "a".repeat(2000);
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.TEXT, content);
        assert result.isValid();
    }

    @Example
    void textPost_oneOverLimit_rejected() {
        String content = "a".repeat(2001);
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.TEXT, content);
        assert !result.isValid();
    }

    @Example
    void matchAnalysis_exactlyAtLimit_accepted() {
        String content = "a".repeat(10000);
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.MATCH_ANALYSIS, content);
        assert result.isValid();
    }

    @Example
    void matchAnalysis_oneOverLimit_rejected() {
        String content = "a".repeat(10001);
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.MATCH_ANALYSIS, content);
        assert !result.isValid();
    }

    @Example
    void pollQuestion_exactlyAtLimit_accepted() {
        String question = "a".repeat(200);
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.POLL, question);
        assert result.isValid();
    }

    @Example
    void pollQuestion_oneOverLimit_rejected() {
        String question = "a".repeat(201);
        PostValidator.ValidationResult result = PostValidator.validateContent(PostType.POLL, question);
        assert !result.isValid();
    }

    @Example
    void imageMetadata_exactlyAtLimit_accepted() {
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata("image/jpeg", 10 * 1024 * 1024);
        assert result.isValid();
    }

    @Example
    void imageMetadata_oneOverLimit_rejected() {
        PostValidator.ValidationResult result = PostValidator.validateImageMetadata("image/jpeg", 10 * 1024 * 1024 + 1);
        assert !result.isValid();
    }

    // =================================================================
    // PROVIDERS
    // =================================================================

    @Provide
    Arbitrary<String> emptyOrBlankStrings() {
        return Arbitraries.of(null, "", "   ", "\t", "\n");
    }

    @Provide
    Arbitrary<List<String>> validPollOptions() {
        Arbitrary<String> option = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(100)
                .filter(s -> !s.isBlank());
        return option.list().ofMinSize(2).ofMaxSize(4);
    }

    @Provide
    Arbitrary<List<String>> tooFewOptions() {
        Arbitrary<String> option = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(100)
                .filter(s -> !s.isBlank());
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(new ArrayList<>()),
                option.list().ofSize(1)
        );
    }

    @Provide
    Arbitrary<List<String>> tooManyOptions() {
        Arbitrary<String> option = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(100)
                .filter(s -> !s.isBlank());
        return option.list().ofMinSize(5).ofMaxSize(8);
    }

    @Provide
    Arbitrary<List<String>> optionsWithOneTooLong() {
        // 2-4 options where at least one exceeds 100 chars
        Arbitrary<String> normalOption = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(100)
                .filter(s -> !s.isBlank());
        Arbitrary<String> longOption = Arbitraries.strings()
                .ofMinLength(101).ofMaxLength(200);

        return Combinators.combine(normalOption, longOption)
                .as((normal, tooLong) -> List.of(normal, tooLong));
    }

    @Provide
    Arbitrary<String> validImageContentType() {
        return Arbitraries.of("image/jpeg", "image/png", "image/webp");
    }

    @Provide
    Arbitrary<String> invalidImageContentType() {
        return Arbitraries.of(
                "image/gif", "image/bmp", "image/svg+xml",
                "application/pdf", "text/plain", "video/mp4",
                null, "", "image/tiff"
        );
    }
}

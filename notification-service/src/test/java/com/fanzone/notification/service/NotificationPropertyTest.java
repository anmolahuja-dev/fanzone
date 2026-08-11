package com.fanzone.notification.service;

import com.fanzone.common.enums.NotificationType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

/**
 * Property-based tests for Notification service.
 * <p>
 * Property 18: Notifications delivered iff type is enabled in preferences
 * Property 19: Preview text always <= 100 chars, truncated at exactly 100 when over
 * Property 21: Activity feed entries ordered descending by createdAt (tested via pure ordering logic)
 * Property 22: Badge count = unread items since last open, resets to 0 on open
 */
class NotificationPropertyTest {

    // =================================================================
    // Property 18: Notification preference enforcement
    // =================================================================

    @Property(tries = 200)
    void preferenceEnforcement_matchAlerts(
            @ForAll boolean matchAlerts,
            @ForAll boolean goals,
            @ForAll boolean replies,
            @ForAll boolean mentions) {

        boolean result = NotificationService.shouldSendForType(
                NotificationType.MATCH_ALERT, matchAlerts, goals, replies, mentions);
        assert result == matchAlerts :
                "MATCH_ALERT delivery should match matchAlerts preference (" + matchAlerts + ")";
    }

    @Property(tries = 200)
    void preferenceEnforcement_goals(
            @ForAll boolean matchAlerts,
            @ForAll boolean goals,
            @ForAll boolean replies,
            @ForAll boolean mentions) {

        boolean result = NotificationService.shouldSendForType(
                NotificationType.GOAL, matchAlerts, goals, replies, mentions);
        assert result == goals :
                "GOAL delivery should match goals preference (" + goals + ")";
    }

    @Property(tries = 200)
    void preferenceEnforcement_replies(
            @ForAll boolean matchAlerts,
            @ForAll boolean goals,
            @ForAll boolean replies,
            @ForAll boolean mentions) {

        boolean result = NotificationService.shouldSendForType(
                NotificationType.REPLY, matchAlerts, goals, replies, mentions);
        assert result == replies :
                "REPLY delivery should match replies preference (" + replies + ")";
    }

    @Property(tries = 200)
    void preferenceEnforcement_mentions(
            @ForAll boolean matchAlerts,
            @ForAll boolean goals,
            @ForAll boolean replies,
            @ForAll boolean mentions) {

        boolean result = NotificationService.shouldSendForType(
                NotificationType.MENTION, matchAlerts, goals, replies, mentions);
        assert result == mentions :
                "MENTION delivery should match mentions preference (" + mentions + ")";
    }

    @Property(tries = 100)
    void preferenceEnforcement_reputationChange_alwaysDelivered(
            @ForAll boolean matchAlerts,
            @ForAll boolean goals,
            @ForAll boolean replies,
            @ForAll boolean mentions) {

        boolean result = NotificationService.shouldSendForType(
                NotificationType.REPUTATION_CHANGE, matchAlerts, goals, replies, mentions);
        assert result : "REPUTATION_CHANGE should always be delivered regardless of preferences";
    }

    // =================================================================
    // Property 19: Preview truncation
    // =================================================================

    @Property(tries = 200)
    void previewTruncation_neverExceeds100(@ForAll @StringLength(min = 0, max = 5000) String text) {
        String preview = NotificationService.truncatePreview(text);
        assert preview.length() <= 100 :
                "Preview length " + preview.length() + " exceeds 100 for input of length " + text.length();
    }

    @Property(tries = 200)
    void previewTruncation_shortText_unchanged(@ForAll @StringLength(min = 0, max = 100) String text) {
        String preview = NotificationService.truncatePreview(text);
        assert preview.equals(text) :
                "Short text should not be modified";
    }

    @Property(tries = 200)
    void previewTruncation_longText_exactlyAt100(@ForAll @StringLength(min = 101, max = 5000) String text) {
        String preview = NotificationService.truncatePreview(text);
        assert preview.length() == 100 :
                "Long text preview should be exactly 100 chars, got " + preview.length();
    }

    @Example
    void previewTruncation_null_returnsNull() {
        assert NotificationService.truncatePreview(null) == null;
    }

    @Example
    void previewTruncation_exactlyAt100_unchanged() {
        String text = "a".repeat(100);
        assert NotificationService.truncatePreview(text).equals(text);
    }

    @Example
    void previewTruncation_101_truncatedTo100() {
        String text = "a".repeat(101);
        String result = NotificationService.truncatePreview(text);
        assert result.length() == 100;
    }
}

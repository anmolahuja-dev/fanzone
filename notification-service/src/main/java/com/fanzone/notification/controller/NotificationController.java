package com.fanzone.notification.controller;

import com.fanzone.common.security.UserPrincipal;
import com.fanzone.notification.dto.ActivityFeedResponse;
import com.fanzone.notification.dto.NotificationPreferenceResponse;
import com.fanzone.notification.dto.UpdatePreferencesRequest;
import com.fanzone.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get activity feed (paginated, reverse chronological).
     * Returns unread count in response header.
     */
    @GetMapping("/activity-feeds")
    public ResponseEntity<List<ActivityFeedResponse>> getActivityFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ActivityFeedResponse> items = notificationService.getActivityFeed(
                principal.getUserId(), page, size);
        long unreadCount = notificationService.getUnreadCount(principal.getUserId());

        return ResponseEntity.ok()
                .header("X-Unread-Count", String.valueOf(unreadCount))
                .body(items);
    }

    /**
     * Mark all activity feed items as read. Resets the unread badge.
     */
    @PutMapping("/activity-feeds/read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Get current notification preferences.
     */
    @GetMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationPreferenceResponse prefs = notificationService.getPreferences(principal.getUserId());
        return ResponseEntity.ok(prefs);
    }

    /**
     * Update notification preferences (per-type toggles).
     */
    @PutMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdatePreferencesRequest request) {

        NotificationPreferenceResponse prefs = notificationService.updatePreferences(
                principal.getUserId(), request);
        return ResponseEntity.ok(prefs);
    }
}

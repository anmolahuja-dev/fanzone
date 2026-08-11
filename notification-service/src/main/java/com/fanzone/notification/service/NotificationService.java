package com.fanzone.notification.service;

import com.fanzone.common.enums.NotificationType;
import com.fanzone.notification.dto.ActivityFeedResponse;
import com.fanzone.notification.dto.NotificationPreferenceResponse;
import com.fanzone.notification.dto.UpdatePreferencesRequest;
import com.fanzone.notification.model.ActivityFeedEntity;
import com.fanzone.notification.model.NotificationPreferenceEntity;
import com.fanzone.notification.repository.ActivityFeedRepository;
import com.fanzone.notification.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_PREVIEW_LENGTH = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationPreferenceRepository preferenceRepository;
    private final ActivityFeedRepository activityFeedRepository;

    public NotificationService(NotificationPreferenceRepository preferenceRepository,
                               ActivityFeedRepository activityFeedRepository) {
        this.preferenceRepository = preferenceRepository;
        this.activityFeedRepository = activityFeedRepository;
    }

    // ---- Preference enforcement ----

    /**
     * Checks if a notification should be delivered based on user preferences.
     */
    public boolean shouldSend(UUID userId, NotificationType type) {
        NotificationPreferenceEntity prefs = preferenceRepository.findById(userId)
                .orElse(defaultPreferences(userId));

        return switch (type) {
            case MATCH_ALERT -> prefs.isMatchAlerts();
            case GOAL -> prefs.isGoals();
            case REPLY -> prefs.isReplies();
            case MENTION -> prefs.isMentions();
            case REPUTATION_CHANGE -> true; // Always deliver reputation changes
        };
    }

    // ---- Activity feed ----

    @Transactional(readOnly = true)
    public List<ActivityFeedResponse> getActivityFeed(UUID userId, int page, int size) {
        int pageSize = size > 0 && size <= 100 ? size : DEFAULT_PAGE_SIZE;
        List<ActivityFeedEntity> items = activityFeedRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, pageSize));

        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return activityFeedRepository.countUnread(userId);
    }

    public void markAllAsRead(UUID userId) {
        activityFeedRepository.markAllAsRead(userId);
        log.debug("Marked all activity items as read for user {}", userId);
    }

    /**
     * Creates an activity feed entry for a user.
     */
    public void createActivityEntry(UUID userId, String activityType, String title,
                                    String preview, UUID referenceId) {
        ActivityFeedEntity entity = new ActivityFeedEntity();
        entity.setUserId(userId);
        entity.setActivityType(activityType);
        entity.setTitle(title);
        entity.setPreview(truncatePreview(preview));
        entity.setReferenceId(referenceId);
        activityFeedRepository.save(entity);
    }

    // ---- Preferences management ----

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        NotificationPreferenceEntity prefs = preferenceRepository.findById(userId)
                .orElse(defaultPreferences(userId));
        return new NotificationPreferenceResponse(
                prefs.isMatchAlerts(), prefs.isGoals(), prefs.isReplies(), prefs.isMentions());
    }

    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        NotificationPreferenceEntity prefs = preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    NotificationPreferenceEntity p = new NotificationPreferenceEntity();
                    p.setUserId(userId);
                    return p;
                });

        if (request.matchAlerts() != null) prefs.setMatchAlerts(request.matchAlerts());
        if (request.goals() != null) prefs.setGoals(request.goals());
        if (request.replies() != null) prefs.setReplies(request.replies());
        if (request.mentions() != null) prefs.setMentions(request.mentions());

        preferenceRepository.save(prefs);
        log.info("Updated notification preferences for user {}", userId);

        return new NotificationPreferenceResponse(
                prefs.isMatchAlerts(), prefs.isGoals(), prefs.isReplies(), prefs.isMentions());
    }

    // ---- Pure logic exposed for property testing ----

    /**
     * Truncates preview text to exactly 100 characters.
     */
    public static String truncatePreview(String text) {
        if (text == null) return null;
        if (text.length() <= MAX_PREVIEW_LENGTH) return text;
        return text.substring(0, MAX_PREVIEW_LENGTH);
    }

    /**
     * Determines if a notification should be sent given preference state.
     * Pure function for property testing.
     */
    public static boolean shouldSendForType(NotificationType type, boolean matchAlerts,
                                            boolean goals, boolean replies, boolean mentions) {
        return switch (type) {
            case MATCH_ALERT -> matchAlerts;
            case GOAL -> goals;
            case REPLY -> replies;
            case MENTION -> mentions;
            case REPUTATION_CHANGE -> true;
        };
    }

    // ---- Helpers ----

    private NotificationPreferenceEntity defaultPreferences(UUID userId) {
        NotificationPreferenceEntity prefs = new NotificationPreferenceEntity();
        prefs.setUserId(userId);
        prefs.setMatchAlerts(true);
        prefs.setGoals(true);
        prefs.setReplies(true);
        prefs.setMentions(true);
        return prefs;
    }

    private ActivityFeedResponse toResponse(ActivityFeedEntity entity) {
        return new ActivityFeedResponse(
                entity.getId(), entity.getActivityType(), entity.getTitle(),
                entity.getPreview(), entity.getReferenceId(),
                entity.isRead(), entity.getCreatedAt());
    }
}

package com.fanzone.notification.dto;

public record NotificationPreferenceResponse(
        boolean matchAlerts,
        boolean goals,
        boolean replies,
        boolean mentions
) {}

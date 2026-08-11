package com.fanzone.notification.dto;

public record UpdatePreferencesRequest(
        Boolean matchAlerts,
        Boolean goals,
        Boolean replies,
        Boolean mentions
) {}

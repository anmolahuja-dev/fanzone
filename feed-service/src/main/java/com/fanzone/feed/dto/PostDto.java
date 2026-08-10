package com.fanzone.feed.dto;

import com.fanzone.common.enums.ReputationLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a post in the feed response.
 * Includes author info for display without additional API calls.
 */
public record PostDto(
        UUID id,
        UUID userId,
        String username,
        ReputationLevel reputationLevel,
        String profilePictureUrl,
        UUID clubId,
        String postType,
        String content,
        String imageUrl,
        int commentCount,
        int upvoteCount,
        Instant createdAt
) {}

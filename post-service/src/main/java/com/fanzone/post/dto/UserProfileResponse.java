package com.fanzone.post.dto;

import com.fanzone.common.enums.ReputationLevel;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        UUID favoriteClubId,
        ReputationLevel reputationLevel,
        int reputation,
        String profilePictureUrl,
        long followerCount,
        long followingCount,
        boolean isFollowing
) {}

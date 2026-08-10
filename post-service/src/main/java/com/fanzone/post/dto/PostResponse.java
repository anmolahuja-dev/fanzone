package com.fanzone.post.dto;

import com.fanzone.common.enums.PostType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a single post.
 */
public record PostResponse(
        UUID id,
        UUID userId,
        UUID clubId,
        PostType postType,
        String content,
        String imageUrl,
        List<PollOptionResponse> pollOptions,
        int commentCount,
        int upvoteCount,
        boolean isFlagged,
        Instant createdAt
) {}

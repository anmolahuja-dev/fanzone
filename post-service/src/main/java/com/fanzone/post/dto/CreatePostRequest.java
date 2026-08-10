package com.fanzone.post.dto;

import com.fanzone.common.enums.PostType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for creating a new post.
 * The content field serves different purposes based on postType:
 * - TEXT: the post body (1-2000 chars)
 * - POLL: the poll question (1-200 chars)
 * - MATCH_ANALYSIS: the analysis body (1-10000 chars)
 * - IMAGE: optional caption (0-2000 chars)
 */
public record CreatePostRequest(
        @NotNull(message = "Post type is required")
        PostType postType,

        String content,

        /** Poll options — only required when postType is POLL */
        List<String> pollOptions
) {}

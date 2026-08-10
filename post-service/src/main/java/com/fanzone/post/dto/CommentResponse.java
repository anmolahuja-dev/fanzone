package com.fanzone.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID userId,
        UUID parentCommentId,
        String content,
        int upvoteCount,
        int nestingLevel,
        Instant createdAt,
        List<CommentResponse> replies
) {}

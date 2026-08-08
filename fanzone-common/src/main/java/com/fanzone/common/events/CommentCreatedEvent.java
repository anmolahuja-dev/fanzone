package com.fanzone.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID postId,
        UUID authorId,
        UUID parentCommentAuthorId,
        List<String> mentionedUsernames,
        String previewText,
        Instant timestamp
) {}

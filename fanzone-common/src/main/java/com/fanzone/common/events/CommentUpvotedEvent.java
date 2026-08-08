package com.fanzone.common.events;

import java.time.Instant;
import java.util.UUID;

public record CommentUpvotedEvent(
        UUID commentId,
        UUID authorId,
        UUID upvoterId,
        Instant timestamp
) {}

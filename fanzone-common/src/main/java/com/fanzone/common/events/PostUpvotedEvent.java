package com.fanzone.common.events;

import java.time.Instant;
import java.util.UUID;

public record PostUpvotedEvent(
        UUID postId,
        UUID authorId,
        UUID upvoterId,
        Instant timestamp
) {}

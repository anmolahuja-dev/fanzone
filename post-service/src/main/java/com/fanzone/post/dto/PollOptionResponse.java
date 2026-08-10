package com.fanzone.post.dto;

import java.util.UUID;

public record PollOptionResponse(
        UUID id,
        String optionText,
        int voteCount,
        int displayOrder
) {}

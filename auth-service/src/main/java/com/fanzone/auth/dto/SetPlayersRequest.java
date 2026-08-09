package com.fanzone.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SetPlayersRequest(
        @NotEmpty(message = "At least 1 player must be selected")
        @Size(max = 5, message = "Maximum 5 players can be selected")
        List<UUID> playerIds
) {}

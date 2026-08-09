package com.fanzone.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetClubRequest(
        @NotNull(message = "Club ID is required")
        UUID clubId
) {}

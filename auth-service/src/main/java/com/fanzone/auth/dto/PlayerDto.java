package com.fanzone.auth.dto;

import java.util.UUID;

public record PlayerDto(
        UUID id,
        String name,
        String photoUrl,
        String position
) {}

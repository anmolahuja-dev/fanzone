package com.fanzone.auth.dto;

import java.util.UUID;

public record ClubDto(
        UUID id,
        String name,
        String logoUrl
) {}

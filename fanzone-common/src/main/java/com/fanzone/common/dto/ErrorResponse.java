package com.fanzone.common.dto;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        String severity,
        String source,
        Instant timestamp,
        String traceId
) {}

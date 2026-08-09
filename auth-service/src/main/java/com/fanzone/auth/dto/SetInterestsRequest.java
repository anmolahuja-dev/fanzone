package com.fanzone.auth.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SetInterestsRequest(
        @NotEmpty(message = "At least 1 interest must be selected")
        List<String> interests
) {}

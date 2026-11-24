package org.jober.jober.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
        @NotNull(message = "jobOfferId is required")
        Long jobOfferId,

        @Size(max = 2000, message = "notes must be up to 2000 characters")
        String notes
) {
}
package org.jober.jober.application.dto;

import jakarta.validation.constraints.NotNull;
import org.jober.jober.common.enums.ApplicationStatus;

public record StatusUpdateRequest(
        @NotNull(message = "status is required")
        ApplicationStatus status
) {
}
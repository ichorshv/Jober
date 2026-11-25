package org.jober.jober.interview.dto;

import jakarta.validation.constraints.NotNull;
import org.jober.jober.common.enums.InterviewType;

import java.time.LocalDateTime;

public record InterviewRequest(
        @NotNull Long applicationId,
        @NotNull InterviewType type,
        @NotNull LocalDateTime scheduledAt,
        String locationOrLink,
        String notes
) {}
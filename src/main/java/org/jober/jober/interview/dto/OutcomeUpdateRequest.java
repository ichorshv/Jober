package org.jober.jober.interview.dto;

import jakarta.validation.constraints.NotNull;
import org.jober.jober.common.enums.InterviewOutcome;

public record OutcomeUpdateRequest(
        @NotNull InterviewOutcome outcome
) {}
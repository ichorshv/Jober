package org.jober.jober.interview.dto;

import org.jober.jober.common.enums.InterviewOutcome;
import org.jober.jober.common.enums.InterviewType;

import java.time.LocalDateTime;

public record InterviewResponse(
        Long id,
        Long applicationId,
        Long studentId,
        String studentName,
        Long jobOfferId,
        String jobOfferTitle,
        Long companyId,
        String companyName,
        InterviewType type,
        InterviewOutcome outcome,
        LocalDateTime scheduledAt,
        String locationOrLink,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
package org.jober.jober.application.dto;

import org.jober.jober.application.Application;
import org.jober.jober.common.enums.ApplicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long studentId,
        String studentName,
        Long jobOfferId,
        String jobOfferTitle,
        Long companyId,
        String companyName,
        ApplicationStatus status,
        LocalDate appliedDate,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getFullName(),
                application.getJobOffer().getId(),
                application.getJobOffer().getTitle(),
                application.getJobOffer().getCompany().getId(),
                application.getJobOffer().getCompany().getName(),
                application.getStatus(),
                application.getAppliedDate(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
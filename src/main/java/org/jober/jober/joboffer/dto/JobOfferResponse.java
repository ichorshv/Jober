
package org.jober.jober.joboffer.dto;

import org.jober.jober.common.enums.JobType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobOfferResponse(

        Long id,
        Long companyId,
        String companyName,
        String title,
        String description,
        JobType jobType,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate deadline,
        boolean active,
        LocalDateTime createdAt

) {
}

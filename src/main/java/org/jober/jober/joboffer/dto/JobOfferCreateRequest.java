
package org.jober.jober.joboffer.dto;

import jakarta.validation.constraints.*;
import org.jober.jober.common.enums.JobType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobOfferCreateRequest(

        Long companyId,

        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        @NotNull
        JobType jobType,

        @Size(max = 150)
        String location,

        @DecimalMin("0.0")
        BigDecimal salaryMin,

        @DecimalMin("0.0")
        BigDecimal salaryMax,

        @FutureOrPresent
        LocalDate deadline

) {
}

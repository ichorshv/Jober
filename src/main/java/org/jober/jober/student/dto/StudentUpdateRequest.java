package org.jober.jober.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Full profile replace (PUT). Password and role are not changed here. */
public record StudentUpdateRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 150)
        String fullName,

        @Size(max = 150)
        String university,

        @Size(max = 100)
        String major,

        @Min(2000)
        @Max(2100)
        Integer graduationYear
) {
}

package org.jober.jober.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

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

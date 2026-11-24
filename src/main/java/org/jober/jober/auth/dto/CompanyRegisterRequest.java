package org.jober.jober.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String website,

        @Size(max = 100)
        String industry,

        @Size(max = 150)
        String location
) {
}

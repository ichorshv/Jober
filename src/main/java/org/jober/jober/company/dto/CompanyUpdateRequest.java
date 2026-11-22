
package org.jober.jober.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String website,

        @Size(max = 100)
        String industry,

        @Size(max = 150)
        String location,

        @NotBlank
        @Email
        String email
) {
}

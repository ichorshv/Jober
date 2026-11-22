
package org.jober.jober.company.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String website,
        String industry,
        String location,
        String email,
        LocalDateTime createdAt
) {
}

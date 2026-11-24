package org.jober.jober.student.dto;

import java.time.LocalDateTime;

import org.jober.jober.common.enums.Role;

public record StudentResponse(
        Long id,
        String email,
        String fullName,
        String university,
        String major,
        Integer graduationYear,
        Role role,
        LocalDateTime createdAt
) {
}

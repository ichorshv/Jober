package org.jober.jober.common.web;

import java.util.Arrays;

import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ForbiddenException;

public record CurrentUser(Long id, UserRole role) {

    public boolean isStudent() {
        return role == UserRole.STUDENT;
    }

    public boolean isCompany() {
        return role == UserRole.COMPANY;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /** Throws 403 unless the caller has one of the given roles. */
    public void requireRole(UserRole... allowed) {
        if (Arrays.stream(allowed).noneMatch(r -> r == role)) {
            throw new ForbiddenException("This action is not allowed for role " + role);
        }
    }

    public void requireAdmin() {
        requireRole(UserRole.ADMIN);
    }
}

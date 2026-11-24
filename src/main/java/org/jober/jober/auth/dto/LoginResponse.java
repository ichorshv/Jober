package org.jober.jober.auth.dto;

import org.jober.jober.common.enums.UserRole;

/** Values the client sends back as X-User-Id and X-User-Role. */
public record LoginResponse(Long id, UserRole role, String fullNameOrName) {
}

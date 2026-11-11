package org.jober.jober.common.web;

import java.util.Locale;

import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Builds a {@link CurrentUser} for any controller parameter of that type. Missing or bad headers give 401. */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String idHeader = webRequest.getHeader(USER_ID_HEADER);
        String roleHeader = webRequest.getHeader(USER_ROLE_HEADER);
        if (idHeader == null || idHeader.isBlank() || roleHeader == null || roleHeader.isBlank()) {
            throw new UnauthorizedException("Headers " + USER_ID_HEADER + " and " + USER_ROLE_HEADER + " are required");
        }

        long id;
        try {
            id = Long.parseLong(idHeader.trim());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException(USER_ID_HEADER + " must be a number");
        }
        if (id <= 0) {
            throw new UnauthorizedException(USER_ID_HEADER + " must be positive");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleHeader.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException(USER_ROLE_HEADER + " must be STUDENT, COMPANY or ADMIN");
        }
        return new CurrentUser(id, role);
    }
}

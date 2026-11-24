package org.jober.jober.auth;

import jakarta.validation.Valid;

import org.jober.jober.auth.dto.CompanyRegisterRequest;
import org.jober.jober.auth.dto.LoginRequest;
import org.jober.jober.auth.dto.LoginResponse;
import org.jober.jober.auth.dto.StudentRegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/register/company")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return authService.registerCompany(request);
    }
}

package org.jober.jober.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jober.jober.auth.dto.CompanyRegisterRequest;
import org.jober.jober.auth.dto.LoginRequest;
import org.jober.jober.auth.dto.LoginResponse;
import org.jober.jober.auth.dto.StudentRegisterRequest;
import org.jober.jober.common.enums.Role;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.UnauthorizedException;
import org.jober.jober.company.Company;
import org.jober.jober.company.CompanyRepository;
import org.jober.jober.student.Student;
import org.jober.jober.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void studentLoginReturnsStudentRole() {
        when(studentRepository.findByEmailIgnoreCase("ivan@student.example"))
                .thenReturn(Optional.of(student(2L, "Ivan Petrov", Role.STUDENT)));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("ivan@student.example", "password123"));

        assertEquals(new LoginResponse(2L, UserRole.STUDENT, "Ivan Petrov"), response);
        verify(companyRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void adminLoginReturnsAdminRole() {
        when(studentRepository.findByEmailIgnoreCase("admin@jobtracker.example"))
                .thenReturn(Optional.of(student(1L, "Admin User", Role.ADMIN)));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("admin@jobtracker.example", "password123"));

        assertEquals(UserRole.ADMIN, response.role());
    }

    @Test
    void companyLoginFallsBackToCompanies() {
        when(studentRepository.findByEmailIgnoreCase("hr@acme.example")).thenReturn(Optional.empty());
        when(companyRepository.findByEmailIgnoreCase("hr@acme.example"))
                .thenReturn(Optional.of(company(1L, "Acme Software")));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("hr@acme.example", "password123"));

        assertEquals(new LoginResponse(1L, UserRole.COMPANY, "Acme Software"), response);
    }

    @Test
    void wrongPasswordGives401() {
        when(studentRepository.findByEmailIgnoreCase("ivan@student.example"))
                .thenReturn(Optional.of(student(2L, "Ivan Petrov", Role.STUDENT)));
        when(passwordEncoder.matches("wrong-pass", "hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("ivan@student.example", "wrong-pass")));
    }

    @Test
    void unknownEmailGives401() {
        when(studentRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        when(companyRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "password123")));
    }

    @Test
    void registerStudentAlwaysCreatesStudentWithHashedPassword() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.registerStudent(new StudentRegisterRequest(
                "new@student.example", "password123", "New Student", "TU Sofia", "CS", 2027));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).saveAndFlush(captor.capture());
        assertEquals(Role.STUDENT, captor.getValue().getRole());
        assertEquals("encoded", captor.getValue().getPasswordHash());
    }

    @Test
    void registerStudentWithCompanyEmailGives409() {
        when(studentRepository.existsByEmailIgnoreCase("hr@acme.example")).thenReturn(false);
        when(companyRepository.existsByEmailIgnoreCase("hr@acme.example")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registerStudent(new StudentRegisterRequest(
                "hr@acme.example", "password123", "Someone", null, null, null)));
        verify(studentRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerCompanyWithStudentEmailGives409() {
        when(studentRepository.existsByEmailIgnoreCase("ivan@student.example")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registerCompany(new CompanyRegisterRequest(
                "ivan@student.example", "password123", "Ivan Ltd", null, null, null)));
        verify(companyRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerCompanyWithTakenNameGives409() {
        when(companyRepository.existsByNameIgnoreCase("Acme Software")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registerCompany(new CompanyRegisterRequest(
                "other@acme.example", "password123", "Acme Software", null, null, null)));
        verify(companyRepository, never()).saveAndFlush(any());
    }

    private static Student student(Long id, String name, Role role) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id);
        s.setFullName(name);
        s.setRole(role);
        s.setPasswordHash("hash");
        return s;
    }

    private static Company company(Long id, String name) {
        Company c = new Company();
        c.setId(id);
        c.setName(name);
        c.setPasswordHash("hash");
        return c;
    }
}

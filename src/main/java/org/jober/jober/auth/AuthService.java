package org.jober.jober.auth;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StudentRepository studentRepository, CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Looks the email up in students first, then in companies. Any failure gives the same 401. */
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();

        Optional<Student> student = studentRepository.findByEmailIgnoreCase(email);
        if (student.isPresent()) {
            Student s = student.get();
            checkPassword(request.password(), s.getPasswordHash());
            UserRole role = s.getRole() == Role.ADMIN ? UserRole.ADMIN : UserRole.STUDENT;
            return new LoginResponse(s.getId(), role, s.getFullName());
        }

        Company company = companyRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        checkPassword(request.password(), company.getPasswordHash());
        return new LoginResponse(company.getId(), UserRole.COMPANY, company.getName());
    }

    /** Public registration always creates a STUDENT; admins are never registered here. */
    @Transactional
    public LoginResponse registerStudent(StudentRegisterRequest request) {
        String email = request.email().trim();
        requireEmailFree(email);

        Student student = new Student();
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(request.password()));
        student.setFullName(request.fullName().trim());
        student.setUniversity(request.university());
        student.setMajor(request.major());
        student.setGraduationYear(request.graduationYear());
        student.setRole(Role.STUDENT);

        Student saved = studentRepository.saveAndFlush(student);
        return new LoginResponse(saved.getId(), UserRole.STUDENT, saved.getFullName());
    }

    @Transactional
    public LoginResponse registerCompany(CompanyRegisterRequest request) {
        String email = request.email().trim();
        String name = request.name().trim();
        requireEmailFree(email);
        if (companyRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Company name already exists");
        }

        Company company = new Company();
        company.setEmail(email);
        company.setPasswordHash(passwordEncoder.encode(request.password()));
        company.setName(name);
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setLocation(request.location());

        Company saved = companyRepository.saveAndFlush(company);
        return new LoginResponse(saved.getId(), UserRole.COMPANY, saved.getName());
    }

    /** An email must be unique across BOTH students and companies. */
    private void requireEmailFree(String email) {
        if (studentRepository.existsByEmailIgnoreCase(email) || companyRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
    }

    private void checkPassword(String raw, String hash) {
        if (!passwordEncoder.matches(raw, hash)) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
    }
}

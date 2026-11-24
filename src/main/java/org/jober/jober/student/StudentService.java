package org.jober.jober.student;

import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.BadRequestException;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.NotFoundException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.company.CompanyRepository;
import org.jober.jober.student.dto.StudentResponse;
import org.jober.jober.student.dto.StudentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;

    public StudentService(StudentRepository studentRepository, CompanyRepository companyRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
    }

    /** STUDENT and ADMIN both live in the students table, so both have a "me" profile. */
    public StudentResponse getMe(CurrentUser user) {
        user.requireRole(UserRole.STUDENT, UserRole.ADMIN);
        return toResponse(findStudent(user.id()));
    }

    @Transactional
    public StudentResponse updateMe(StudentUpdateRequest request, CurrentUser user) {
        user.requireRole(UserRole.STUDENT, UserRole.ADMIN);
        Student student = findStudent(user.id());

        String email = request.email().trim();
        if (!email.equalsIgnoreCase(student.getEmail())) {
            boolean taken = studentRepository.existsByEmailIgnoreCase(email)
                    || companyRepository.existsByEmailIgnoreCase(email);
            if (taken) {
                throw new ConflictException("Email is already registered");
            }
        }

        student.setEmail(email);
        student.setFullName(request.fullName().trim());
        student.setUniversity(request.university());
        student.setMajor(request.major());
        student.setGraduationYear(request.graduationYear());
        return toResponse(studentRepository.saveAndFlush(student));
    }

    public Page<StudentResponse> search(String query, Pageable pageable, CurrentUser user) {
        user.requireAdmin();
        Page<Student> page = query == null || query.isBlank()
                ? studentRepository.findAll(pageable)
                : studentRepository.search(query.trim(), pageable);
        return page.map(StudentService::toResponse);
    }

    public StudentResponse getById(Long id, CurrentUser user) {
        user.requireAdmin();
        return toResponse(findStudent(id));
    }

    /** Applications and their interviews are removed by ON DELETE CASCADE. */
    @Transactional
    public void delete(Long id, CurrentUser user) {
        user.requireAdmin();
        if (id.equals(user.id())) {
            throw new BadRequestException("Admins cannot delete their own account");
        }
        studentRepository.delete(findStudent(id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student not found: " + id));
    }

    private static StudentResponse toResponse(Student s) {
        return new StudentResponse(s.getId(), s.getEmail(), s.getFullName(), s.getUniversity(), s.getMajor(),
                s.getGraduationYear(), s.getRole(), s.getCreatedAt());
    }
}

package org.jober.jober.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.BadRequestException;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.exception.NotFoundException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.company.CompanyRepository;
import org.jober.jober.student.dto.StudentUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    private static final CurrentUser ADMIN = new CurrentUser(1L, UserRole.ADMIN);
    private static final CurrentUser STUDENT = new CurrentUser(2L, UserRole.STUDENT);
    private static final CurrentUser COMPANY = new CurrentUser(1L, UserRole.COMPANY);

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void studentSeesOwnProfileOnly() {
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student(2L, "ivan@student.example")));

        assertEquals(2L, studentService.getMe(STUDENT).id());
    }

    @Test
    void companyHasNoStudentProfile() {
        assertThrows(ForbiddenException.class, () -> studentService.getMe(COMPANY));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void studentCannotSearchStudents() {
        assertThrows(ForbiddenException.class, () -> studentService.search("ivan", Pageable.unpaged(), STUDENT));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void studentCannotGetOtherStudentById() {
        assertThrows(ForbiddenException.class, () -> studentService.getById(3L, STUDENT));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void studentCannotDeleteStudents() {
        assertThrows(ForbiddenException.class, () -> studentService.delete(3L, STUDENT));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void adminCannotDeleteSelf() {
        assertThrows(BadRequestException.class, () -> studentService.delete(1L, ADMIN));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void adminGetsMissingStudent404() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> studentService.getById(99L, ADMIN));
    }

    @Test
    void updateMeToCompanyEmailGives409() {
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student(2L, "ivan@student.example")));
        when(studentRepository.existsByEmailIgnoreCase("hr@acme.example")).thenReturn(false);
        when(companyRepository.existsByEmailIgnoreCase("hr@acme.example")).thenReturn(true);

        assertThrows(ConflictException.class, () -> studentService.updateMe(
                new StudentUpdateRequest("hr@acme.example", "Ivan", null, null, null), STUDENT));
        verify(studentRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateMeKeepingOwnEmailSkipsUniquenessCheck() {
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student(2L, "ivan@student.example")));
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = studentService.updateMe(
                new StudentUpdateRequest("IVAN@student.example", "Ivan P.", "SU", "CS", 2027), STUDENT);

        assertEquals("Ivan P.", response.fullName());
        verify(studentRepository, never()).existsByEmailIgnoreCase(any());
    }

    private static Student student(Long id, String email) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id);
        s.setEmail(email);
        s.setFullName("Ivan Petrov");
        return s;
    }
}

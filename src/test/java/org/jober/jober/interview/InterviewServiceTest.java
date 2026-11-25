package org.jober.jober.interview;

import org.jober.jober.application.Application;
import org.jober.jober.application.ApplicationRepository;
import org.jober.jober.common.enums.InterviewOutcome;
import org.jober.jober.common.enums.InterviewType;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.company.Company;
import org.jober.jober.interview.dto.InterviewRequest;
import org.jober.jober.interview.dto.OutcomeUpdateRequest;
import org.jober.jober.joboffer.JobOffer;
import org.jober.jober.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    private InterviewService interviewService;

    private CurrentUser ownerCompany;
    private CurrentUser otherCompany;
    private CurrentUser ownerStudent;
    private CurrentUser otherStudent;
    private CurrentUser adminUser;

    private Application application1;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewService(interviewRepository, applicationRepository);

        ownerCompany = new CurrentUser(1L, UserRole.COMPANY);
        otherCompany = new CurrentUser(2L, UserRole.COMPANY);
        ownerStudent = new CurrentUser(2L, UserRole.STUDENT);
        otherStudent = new CurrentUser(3L, UserRole.STUDENT);
        adminUser = new CurrentUser(99L, UserRole.ADMIN);

        Company company1 = new Company();
        ReflectionTestUtils.setField(company1, "id", 1L);
        company1.setName("Acme Software");

        JobOffer offer1 = new JobOffer();
        ReflectionTestUtils.setField(offer1, "id", 10L);
        offer1.setTitle("Backend Developer");
        offer1.setCompany(company1);

        Student student2 = new Student();
        ReflectionTestUtils.setField(student2, "id", 2L);
        student2.setFullName("Ivan Petrov");

        application1 = new Application();
        ReflectionTestUtils.setField(application1, "id", 100L);
        application1.setJobOffer(offer1);
        application1.setStudent(student2);
    }

    @Test
    void createInterview_byOwnerCompany_shouldCreateWithPendingOutcome() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application1));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 500L);
            return saved;
        });

        InterviewRequest request = new InterviewRequest(
                100L,
                InterviewType.TECHNICAL,
                LocalDateTime.of(2026, 12, 1, 14, 30),
                "https://meet.example.com/room1",
                "Coding round"
        );

        var response = interviewService.createInterview(ownerCompany, request);

        assertEquals(500L, response.id());
        assertEquals(100L, response.applicationId());
        assertEquals(InterviewType.TECHNICAL, response.type());
        assertEquals(InterviewOutcome.PENDING, response.outcome());
        assertEquals("https://meet.example.com/room1", response.locationOrLink());
        verify(interviewRepository).save(any(Interview.class));
    }

    @Test
    void createInterview_byOtherCompanyOrStudent_shouldThrowForbidden() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application1));

        InterviewRequest request = new InterviewRequest(
                100L,
                InterviewType.HR,
                LocalDateTime.of(2026, 12, 2, 10, 0),
                "Office",
                null
        );

        assertThrows(ForbiddenException.class, () -> interviewService.createInterview(otherCompany, request));
        assertThrows(ForbiddenException.class, () -> interviewService.createInterview(ownerStudent, request));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateOutcome_byOwnerCompany_shouldUpdateOutcome() {
        Interview interview = new Interview();
        ReflectionTestUtils.setField(interview, "id", 500L);
        interview.setApplication(application1);
        interview.setType(InterviewType.TECHNICAL);
        interview.setOutcome(InterviewOutcome.PENDING);
        interview.setScheduledAt(LocalDateTime.of(2026, 12, 1, 14, 30));

        when(interviewRepository.findById(500L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = interviewService.updateOutcome(ownerCompany, 500L, new OutcomeUpdateRequest(InterviewOutcome.PASSED));

        assertEquals(InterviewOutcome.PASSED, response.outcome());
    }

    @Test
    void updateOutcome_byOtherCompanyOrStudent_shouldThrowForbidden() {
        Interview interview = new Interview();
        ReflectionTestUtils.setField(interview, "id", 500L);
        interview.setApplication(application1);
        interview.setOutcome(InterviewOutcome.PENDING);

        when(interviewRepository.findById(500L)).thenReturn(Optional.of(interview));

        assertThrows(ForbiddenException.class,
                () -> interviewService.updateOutcome(otherCompany, 500L, new OutcomeUpdateRequest(InterviewOutcome.FAILED)));
        assertThrows(ForbiddenException.class,
                () -> interviewService.updateOutcome(ownerStudent, 500L, new OutcomeUpdateRequest(InterviewOutcome.FAILED)));
    }

    @Test
    void getInterviewsByApplication_allowedForOwnerStudentOwnerCompanyAndAdmin() {
        Interview interview = new Interview();
        ReflectionTestUtils.setField(interview, "id", 500L);
        interview.setApplication(application1);
        interview.setType(InterviewType.HR);
        interview.setOutcome(InterviewOutcome.PENDING);
        interview.setScheduledAt(LocalDateTime.of(2026, 12, 3, 11, 0));

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application1));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(100L)).thenReturn(List.of(interview));

        assertEquals(1, interviewService.getInterviewsByApplication(ownerStudent, 100L).size());
        assertEquals(1, interviewService.getInterviewsByApplication(ownerCompany, 100L).size());
        assertEquals(1, interviewService.getInterviewsByApplication(adminUser, 100L).size());
    }

    @Test
    void getInterviewsByApplication_forbiddenForOtherStudentOrOtherCompany() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application1));

        assertThrows(ForbiddenException.class, () -> interviewService.getInterviewsByApplication(otherStudent, 100L));
        assertThrows(ForbiddenException.class, () -> interviewService.getInterviewsByApplication(otherCompany, 100L));
    }
}
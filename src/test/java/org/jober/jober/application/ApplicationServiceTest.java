package org.jober.jober.application;

import org.jober.jober.application.dto.ApplicationRequest;
import org.jober.jober.application.dto.StatusUpdateRequest;
import org.jober.jober.common.enums.ApplicationStatus;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.BadRequestException;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.company.Company;
import org.jober.jober.joboffer.JobOffer;
import org.jober.jober.joboffer.JobOfferRepository;
import org.jober.jober.student.Student;
import org.jober.jober.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private JobOfferRepository jobOfferRepository;

    private Clock clock;
    private ApplicationService applicationService;

    private Student student2;
    private Company company1;
    private JobOffer offer1;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
        applicationService = new ApplicationService(applicationRepository, studentRepository, jobOfferRepository, clock);

        student2 = new Student();
        ReflectionTestUtils.setField(student2, "id", 2L);
        student2.setFullName("Ivan Petrov");

        company1 = new Company();
        company1.setId(1L);
        company1.setName("Acme Software");

        offer1 = new JobOffer();
        offer1.setId(1L);
        offer1.setTitle("Java Intern");
        offer1.setCompany(company1);
        offer1.setActive(true);
        offer1.setDeadline(LocalDate.now(clock).plusDays(10));
    }

    @Test
    void createApplication_shouldSaveWithSavedStatus() {
        CurrentUser studentUser = new CurrentUser(2L, UserRole.STUDENT);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer1));
        when(applicationRepository.existsByStudentIdAndJobOfferId(2L, 1L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        var response = applicationService.createApplication(studentUser, new ApplicationRequest(1L, "Interested"));

        assertEquals(ApplicationStatus.SAVED, response.status());
        assertNull(response.appliedDate());
        assertEquals("Interested", response.notes());
    }

    @Test
    void createApplication_duplicate_shouldThrow409() {
        CurrentUser studentUser = new CurrentUser(2L, UserRole.STUDENT);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer1));
        when(applicationRepository.existsByStudentIdAndJobOfferId(2L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> applicationService.createApplication(studentUser, new ApplicationRequest(1L, null)));
    }

    @Test
    void createApplication_inactiveOrExpiredOffer_shouldThrow400() {
        CurrentUser studentUser = new CurrentUser(2L, UserRole.STUDENT);
        offer1.setActive(false);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer1));

        assertThrows(BadRequestException.class,
                () -> applicationService.createApplication(studentUser, new ApplicationRequest(1L, null)));

        offer1.setActive(true);
        offer1.setDeadline(LocalDate.now(clock).minusDays(1));

        assertThrows(BadRequestException.class,
                () -> applicationService.createApplication(studentUser, new ApplicationRequest(1L, null)));
    }

    @Test
    void apply_fromSaved_shouldSetAppliedAndAppliedDateAndUpdatedAt() {
        Application app = buildApplication(10L, student2, offer1, ApplicationStatus.SAVED, LocalDateTime.now(clock).minusDays(2));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = applicationService.applyToApplication(new CurrentUser(2L, UserRole.STUDENT), 10L);

        assertEquals(ApplicationStatus.APPLIED, response.status());
        assertEquals(LocalDate.now(clock), response.appliedDate());
        assertEquals(LocalDateTime.now(clock), response.updatedAt());
    }

    @Test
    void apply_notOwner_shouldThrow403() {
        Application app = buildApplication(10L, student2, offer1, ApplicationStatus.SAVED, LocalDateTime.now(clock).minusDays(2));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(app));

        assertThrows(ForbiddenException.class,
                () -> applicationService.applyToApplication(new CurrentUser(3L, UserRole.STUDENT), 10L));
    }

    @Test
    void withdraw_allowedFromSavedAppliedInterviewingOffer_andForbiddenFromTerminal() {
        CurrentUser ownerStudent = new CurrentUser(2L, UserRole.STUDENT);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        for (ApplicationStatus allowedFrom : new ApplicationStatus[]{
                ApplicationStatus.SAVED,
                ApplicationStatus.APPLIED,
                ApplicationStatus.INTERVIEWING,
                ApplicationStatus.OFFER
        }) {
            Application app = buildApplication(10L, student2, offer1, allowedFrom, LocalDateTime.now(clock).minusDays(3));
            when(applicationRepository.findById(10L)).thenReturn(Optional.of(app));

            var res = applicationService.withdrawApplication(ownerStudent, 10L);
            assertEquals(ApplicationStatus.WITHDRAWN, res.status());
            assertEquals(LocalDateTime.now(clock), res.updatedAt());
        }

        for (ApplicationStatus terminal : new ApplicationStatus[]{
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN
        }) {
            Application app = buildApplication(10L, student2, offer1, terminal, LocalDateTime.now(clock).minusDays(3));
            when(applicationRepository.findById(10L)).thenReturn(Optional.of(app));

            assertThrows(BadRequestException.class,
                    () -> applicationService.withdrawApplication(ownerStudent, 10L));
        }
    }

    @Test
    void companyUpdateStatus_validTransitionsAndOwnership() {
        CurrentUser company1User = new CurrentUser(1L, UserRole.COMPANY);
        CurrentUser company2User = new CurrentUser(2L, UserRole.COMPANY);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application appliedApp = buildApplication(20L, student2, offer1, ApplicationStatus.APPLIED, LocalDateTime.now(clock).minusDays(5));
        when(applicationRepository.findById(20L)).thenReturn(Optional.of(appliedApp));

        assertThrows(ForbiddenException.class,
                () -> applicationService.updateStatusByCompany(company2User, 20L, new StatusUpdateRequest(ApplicationStatus.INTERVIEWING)));

        var toInterviewing = applicationService.updateStatusByCompany(company1User, 20L, new StatusUpdateRequest(ApplicationStatus.INTERVIEWING));
        assertEquals(ApplicationStatus.INTERVIEWING, toInterviewing.status());
        assertEquals(LocalDateTime.now(clock), toInterviewing.updatedAt());

        Application interviewingApp = buildApplication(21L, student2, offer1, ApplicationStatus.INTERVIEWING, LocalDateTime.now(clock).minusDays(2));
        when(applicationRepository.findById(21L)).thenReturn(Optional.of(interviewingApp));

        var toOffer = applicationService.updateStatusByCompany(company1User, 21L, new StatusUpdateRequest(ApplicationStatus.OFFER));
        assertEquals(ApplicationStatus.OFFER, toOffer.status());

        Application savedApp = buildApplication(22L, student2, offer1, ApplicationStatus.SAVED, LocalDateTime.now(clock).minusDays(2));
        when(applicationRepository.findById(22L)).thenReturn(Optional.of(savedApp));

        assertThrows(BadRequestException.class,
                () -> applicationService.updateStatusByCompany(company1User, 22L, new StatusUpdateRequest(ApplicationStatus.INTERVIEWING)));
    }

    private Application buildApplication(Long id, Student student, JobOffer offer, ApplicationStatus status, LocalDateTime updatedAt) {
        Application a = new Application();
        a.setId(id);
        a.setStudent(student);
        a.setJobOffer(offer);
        a.setStatus(status);
        a.setCreatedAt(LocalDateTime.now(clock).minusDays(10));
        a.setUpdatedAt(updatedAt);
        return a;
    }
}
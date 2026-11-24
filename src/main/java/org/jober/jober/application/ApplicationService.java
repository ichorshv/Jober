package org.jober.jober.application;

import org.jober.jober.application.dto.ApplicationRequest;
import org.jober.jober.application.dto.ApplicationResponse;
import org.jober.jober.application.dto.StatusUpdateRequest;
import org.jober.jober.common.enums.ApplicationStatus;
import org.jober.jober.common.exception.BadRequestException;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.exception.NotFoundException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.common.web.PageResponse;
import org.jober.jober.joboffer.JobOffer;
import org.jober.jober.joboffer.JobOfferRepository;
import org.jober.jober.student.Student;
import org.jober.jober.student.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class ApplicationService {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ApplicationStatus.SAVED, EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.APPLIED, EnumSet.of(ApplicationStatus.INTERVIEWING, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.INTERVIEWING, EnumSet.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OFFER, EnumSet.of(ApplicationStatus.WITHDRAWN),
            ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class)
    );

    private static final Set<ApplicationStatus> COMPANY_ALLOWED_TARGETS = EnumSet.of(
            ApplicationStatus.INTERVIEWING,
            ApplicationStatus.OFFER,
            ApplicationStatus.REJECTED
    );

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final JobOfferRepository jobOfferRepository;
    private final Clock clock;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            StudentRepository studentRepository,
            JobOfferRepository jobOfferRepository,
            Clock clock
    ) {
        this.applicationRepository =applicationRepository;
        this.studentRepository = studentRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.clock = clock;
    }

    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(CurrentUser currentUser, ApplicationRequest request) {
        if (!currentUser.isStudent()) {
            throw new ForbiddenException("Only students can create application");
        }

        Student student = studentRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Student not found with id: " + currentUser.id()));

        JobOffer jobOffer = jobOfferRepository.findById(request.jobOfferId())
                .orElseThrow(() -> new NotFoundException("Job offer not found with id: " + request.jobOfferId()));

        LocalDate today = LocalDate.now(clock);
        if (!Boolean.TRUE.equals(jobOffer.isActive()) || (jobOffer.getDeadline() != null && jobOffer.getDeadline().isBefore(today))) {
            throw new BadRequestException("Cannot apply to an inactive or expired job offer");
        }

        if (applicationRepository.existsByStudentIdAndJobOfferId(student.getId(), jobOffer.getId())) {
            throw new ConflictException("Application already exists for this student and job offer");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Application application = new Application();
        application.setStudent(student);
        application.setJobOffer(jobOffer);
        application.setStatus(ApplicationStatus.SAVED);
        application.setAppliedDate(null);
        application.setNotes(request.notes());
        application.setCreatedAt(now);
        application.setUpdatedAt(now);

        Application saved = applicationRepository.save(application);
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getMyApplications(CurrentUser currentUser, Pageable pageable) {
        if (!currentUser.isStudent()) {
            throw new ForbiddenException("Only students can view their own applications");
        }

        Page<ApplicationResponse> page = applicationRepository.findByStudentId(currentUser.id(), pageable)
                .map(ApplicationResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ApplicationResponse applyToApplication(CurrentUser currentUser, Long applicationId) {
        if (!currentUser.isStudent()) {
            throw new ForbiddenException("Only students can submit applications");
        }

        Application application = findApplicationOrThrow(applicationId);
        requireStudentOwner(currentUser, application);

        if (application.getStatus() != ApplicationStatus.SAVED) {
            throw new BadRequestException("Invalid status transition from " + application.getStatus() + " to APPLIED");
        }

        LocalDate today = LocalDate.now(clock);
        JobOffer jobOffer = application.getJobOffer();
        if (!Boolean.TRUE.equals(jobOffer.isActive()) || (jobOffer.getDeadline() != null && jobOffer.getDeadline().isBefore(today))) {
            throw new BadRequestException("Cannot apply to an inactive or expired job offer");
        }

        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedDate(today);
        application.setUpdatedAt(LocalDateTime.now(clock));

        return ApplicationResponse.from(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationResponse withdrawApplication(CurrentUser currentUser, Long applicationId) {
        if (!currentUser.isStudent()) {
            throw new ForbiddenException("Only students can withdraw applications");
        }

        Application application = findApplicationOrThrow(applicationId);
        requireStudentOwner(currentUser, application);

        validateTransition(application.getStatus(), ApplicationStatus.WITHDRAWN);

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setUpdatedAt(LocalDateTime.now(clock));

        return ApplicationResponse.from(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getCompanyApplications(CurrentUser currentUser, Pageable pageable) {
        if (currentUser.isAdmin()) {
            Page<ApplicationResponse> all = applicationRepository.findAll(pageable)
                    .map(ApplicationResponse::from);
            return PageResponse.from(all);
        }

        if (currentUser.isCompany()) {
            Page<ApplicationResponse> own = applicationRepository.findAllByCompanyId(currentUser.id(), pageable)
                    .map(ApplicationResponse::from);
            return PageResponse.from(own);
        }

        throw new ForbiddenException("Students are not allowed to access company or all applications");
    }

    @Transactional
    public ApplicationResponse updateStatusByCompany(CurrentUser currentUser, Long applicationId, StatusUpdateRequest request) {
        if (!currentUser.isCompany()) {
            throw new ForbiddenException("Only companies can change application status");
        }

        Application application = findApplicationOrThrow(applicationId);
        Long offerCompanyId = application.getJobOffer().getCompany().getId();
        if (!offerCompanyId.equals(currentUser.id())) {
            throw new ForbiddenException("Company does not own the job offer for this application");
        }

        ApplicationStatus targetStatus = request.status();
        if (!COMPANY_ALLOWED_TARGETS.contains(targetStatus)) {
            throw new BadRequestException("Company cannot transition application to status: " + targetStatus);
        }

        validateTransition(application.getStatus(), targetStatus);

        application.setStatus(targetStatus);
        application.setUpdatedAt(LocalDateTime.now(clock));

        return ApplicationResponse.from(applicationRepository.save(application));
    }

    private Application findApplicationOrThrow(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found with id: " + applicationId));
    }

    private void requireStudentOwner(CurrentUser currentUser, Application application) {
        if (!application.getStudent().getId().equals(currentUser.id())) {
            throw new ForbiddenException("Student does not own this application");
        }
    }

    private void validateTransition(ApplicationStatus currentStatus, ApplicationStatus nextStatus) {
        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(nextStatus)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + nextStatus);
        }
    }
}

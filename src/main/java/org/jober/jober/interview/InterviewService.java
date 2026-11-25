package org.jober.jober.interview;

import org.jober.jober.application.Application;
import org.jober.jober.application.ApplicationRepository;
import org.jober.jober.common.enums.InterviewOutcome;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.exception.NotFoundException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.interview.dto.InterviewRequest;
import org.jober.jober.interview.dto.InterviewResponse;
import org.jober.jober.interview.dto.OutcomeUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;

    public InterviewService(InterviewRepository interviewRepository,
                            ApplicationRepository applicationRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public InterviewResponse createInterview(CurrentUser currentUser, InterviewRequest request) {
        requireRole(currentUser, UserRole.COMPANY);

        Application application = findApplicationOrThrow(request.applicationId());
        requireCompanyOwnsApplication(currentUser, application);

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setType(request.type());
        interview.setOutcome(InterviewOutcome.PENDING);
        interview.setScheduledAt(request.scheduledAt());
        interview.setLocationOrLink(request.locationOrLink());
        interview.setNotes(request.notes());

        Interview saved = interviewRepository.save(interview);
        return toResponse(saved);
    }

    @Transactional
    public InterviewResponse updateOutcome(CurrentUser currentUser, Long interviewId, OutcomeUpdateRequest request) {
        requireRole(currentUser, UserRole.COMPANY);

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new NotFoundException("Interview not found: " + interviewId));

        requireCompanyOwnsApplication(currentUser, interview.getApplication());
        interview.setOutcome(request.outcome());

        Interview saved = interviewRepository.save(interview);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(CurrentUser currentUser, Long applicationId) {
        Application application = findApplicationOrThrow(applicationId);

        boolean allowed = switch (currentUser.role()) {
            case ADMIN -> true;
            case STUDENT -> application.getStudent() != null
                    && currentUser.id().equals(application.getStudent().getId());
            case COMPANY -> application.getJobOffer() != null
                    && application.getJobOffer().getCompany() != null
                    && currentUser.id().equals(application.getJobOffer().getCompany().getId());
        };

        if (!allowed) {
            throw new ForbiddenException("Access denied to interviews for this application");
        }

        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Application findApplicationOrThrow(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found: " + applicationId));
    }

    private void requireRole(CurrentUser currentUser, UserRole expectedRole) {
        if (currentUser == null || currentUser.role() != expectedRole) {
            throw new ForbiddenException("Only " + expectedRole + " is allowed for this action");
        }
    }

    private void requireCompanyOwnsApplication(CurrentUser currentUser, Application application) {
        Long companyId = application.getJobOffer() != null && application.getJobOffer().getCompany() != null
                ? application.getJobOffer().getCompany().getId()
                : null;
        if (companyId == null || !currentUser.id().equals(companyId)) {
            throw new ForbiddenException("Company does not own the job offer for this application");
        }
    }

    private InterviewResponse toResponse(Interview interview) {
        Application app = interview.getApplication();
        Long studentId = app.getStudent() != null ? app.getStudent().getId() : null;
        String studentName = app.getStudent() != null ? app.getStudent().getFullName() : null;
        Long jobOfferId = app.getJobOffer() != null ? app.getJobOffer().getId() : null;
        String jobOfferTitle = app.getJobOffer() != null ? app.getJobOffer().getTitle() : null;
        Long companyId = app.getJobOffer() != null && app.getJobOffer().getCompany() != null
                ? app.getJobOffer().getCompany().getId()
                : null;
        String companyName = app.getJobOffer() != null && app.getJobOffer().getCompany() != null
                ? app.getJobOffer().getCompany().getName()
                : null;

        return new InterviewResponse(
                interview.getId(),
                app.getId(),
                studentId,
                studentName,
                jobOfferId,
                jobOfferTitle,
                companyId,
                companyName,
                interview.getType(),
                interview.getOutcome(),
                interview.getScheduledAt(),
                interview.getLocationOrLink(),
                interview.getNotes()
        );
    }
}
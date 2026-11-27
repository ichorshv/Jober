
package org.jober.jober.joboffer;

import org.jober.jober.company.Company;
import org.jober.jober.company.CompanyRepository;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.BadRequestException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.exception.NotFoundException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.joboffer.dto.JobOfferCreateRequest;
import org.jober.jober.joboffer.dto.JobOfferUpdateRequest;
import org.jober.jober.joboffer.dto.JobOfferResponse;
import org.jober.jober.common.enums.ApplicationStatus;
import org.jober.jober.application.ApplicationRepository;
import org.jober.jober.common.exception.ConflictException;
import java.util.List;

import org.jober.jober.common.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.jober.jober.joboffer.dto.JobOfferStats;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;

   public JobOfferService(
        JobOfferRepository jobOfferRepository,
        CompanyRepository companyRepository,
        ApplicationRepository applicationRepository) {

        this.jobOfferRepository = jobOfferRepository;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
}


public Page<JobOfferResponse> getAll(
        JobType type,
        String location,
        Long companyId,
        BigDecimal minSalary,
        Boolean active,
        Pageable pageable) {

    if (minSalary != null && minSalary.signum() < 0) {
        throw new BadRequestException(
                "Minimum salary cannot be negative");
    }

    Specification<JobOffer> spec =
            (root, query, cb) -> cb.conjunction();

    if (type != null) {
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("jobType"), type));
    }

    if (location != null && !location.isBlank()) {
        spec = spec.and((root, query, cb) ->
                cb.like(
                        cb.lower(root.get("location")),
                        "%" + location.toLowerCase().trim() + "%"
                ));
    }

    if (companyId != null) {
        spec = spec.and((root, query, cb) ->
                cb.equal(
                        root.get("company").get("id"),
                        companyId
                ));
    }

    if (minSalary != null) {
        spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(
                        root.get("salaryMin"),
                        minSalary
                ));
    }

    if (active != null) {
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("active"), active));
    }

    return jobOfferRepository.findAll(spec, pageable)
            .map(this::toResponse);
}

    // Get popular job offers based on application count
public List<JobOfferStats> getPopularJobOffers(
        long minApplications) {

    if (minApplications < 1) {
        throw new BadRequestException(
                "Minimum applications must be at least 1");
    }

    return jobOfferRepository.findPopularJobOffers(
            minApplications,
            ApplicationStatus.SAVED
    );
}

    // Get active offers the current student has not applied to
    public Page<JobOfferResponse> getNotAppliedJobOffers(
            CurrentUser user,
            Pageable pageable) {

        // Only students can access this functionality
        user.requireRole(UserRole.STUDENT);

        return jobOfferRepository.findNotAppliedJobOffers(
                user.id(),
                ApplicationStatus.SAVED,
                pageable
        ).map(this::toResponse);
    }

    // Get job offer by ID
    public JobOfferResponse getById(Long id) {
        return toResponse(findJobOffer(id));
    }

    // Create new job offer
    @Transactional
    public JobOfferResponse create(
            JobOfferCreateRequest request,
            CurrentUser user) {

        user.requireRole(UserRole.COMPANY, UserRole.ADMIN);

        Long companyId;

        if (user.isAdmin()) {
            if (request.companyId() == null) {
                throw new BadRequestException(
                        "Company ID is required for admin");
            }

            companyId = request.companyId();

        } else {
            companyId = user.id();

            if (request.companyId() != null
                    && !request.companyId().equals(user.id())) {
                throw new ForbiddenException(
                        "Cannot create offers for another company");
            }
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Company not found: " + companyId));

        validateSalary(request.salaryMin(), request.salaryMax());
        validateDeadline(request.deadline());

        JobOffer jobOffer = new JobOffer();

        jobOffer.setCompany(company);
        jobOffer.setTitle(request.title());
        jobOffer.setDescription(request.description());
        jobOffer.setJobType(request.jobType());
        jobOffer.setLocation(request.location());
        jobOffer.setSalaryMin(request.salaryMin());
        jobOffer.setSalaryMax(request.salaryMax());
        jobOffer.setDeadline(request.deadline());
        jobOffer.setActive(true);

        JobOffer saved = jobOfferRepository.saveAndFlush(jobOffer);

        return toResponse(saved);
    }

    // Update existing job offer
    @Transactional
    public JobOfferResponse update(
            Long id,
            JobOfferUpdateRequest request,
            CurrentUser user) {

        user.requireRole(UserRole.COMPANY, UserRole.ADMIN);

        JobOffer jobOffer = findJobOffer(id);

        checkOwnership(jobOffer, user);

        validateSalary(request.salaryMin(), request.salaryMax());
        validateDeadline(request.deadline());

        jobOffer.setTitle(request.title());
        jobOffer.setDescription(request.description());
        jobOffer.setJobType(request.jobType());
        jobOffer.setLocation(request.location());
        jobOffer.setSalaryMin(request.salaryMin());
        jobOffer.setSalaryMax(request.salaryMax());
        jobOffer.setDeadline(request.deadline());

        JobOffer saved = jobOfferRepository.saveAndFlush(jobOffer);

        return toResponse(saved);
    }

    // Close a job offer
    @Transactional
    public JobOfferResponse close(Long id, CurrentUser user) {

        user.requireRole(UserRole.COMPANY, UserRole.ADMIN);

        JobOffer jobOffer = findJobOffer(id);

        checkOwnership(jobOffer, user);

        jobOffer.setActive(false);

        JobOffer saved = jobOfferRepository.saveAndFlush(jobOffer);

        return toResponse(saved);
    }

    // Delete a job offer
    @Transactional
    public void delete(Long id, CurrentUser user) {

    user.requireRole(UserRole.COMPANY, UserRole.ADMIN);

    JobOffer jobOffer = findJobOffer(id);

    checkOwnership(jobOffer, user);

    if (applicationRepository.existsByJobOfferId(id)) {
        throw new ConflictException(
                "Cannot delete job offer with existing applications");
    }

    jobOfferRepository.delete(jobOffer);
}

    // Find offer or throw 404
    private JobOffer findJobOffer(Long id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Job offer not found: " + id));
    }

    // Check company ownership
    private void checkOwnership(
            JobOffer jobOffer,
            CurrentUser user) {

        if (user.isAdmin()) {
            return;
        }

        if (!jobOffer.getCompany().getId().equals(user.id())) {
            throw new ForbiddenException(
                    "You do not own this job offer");
        }
    }

    // Validate salary range
    private void validateSalary(
            BigDecimal salaryMin,
            BigDecimal salaryMax) {

        if (salaryMin != null
                && salaryMin.signum() < 0) {
            throw new BadRequestException(
                    "Minimum salary cannot be negative");
        }

        if (salaryMax != null
                && salaryMax.signum() < 0) {
            throw new BadRequestException(
                    "Maximum salary cannot be negative");
        }

        if (salaryMin != null
                && salaryMax != null
                && salaryMax.compareTo(salaryMin) < 0) {

            throw new BadRequestException(
                    "Maximum salary cannot be lower than minimum salary");
        }
    }

    // Validate deadline
    private void validateDeadline(LocalDate deadline) {

        if (deadline != null
                && deadline.isBefore(LocalDate.now())) {

            throw new BadRequestException(
                    "Deadline cannot be in the past");
        }
    }

    // Convert entity to response DTO
    private JobOfferResponse toResponse(JobOffer jobOffer) {

        return new JobOfferResponse(
                jobOffer.getId(),
                jobOffer.getCompany().getId(),
                jobOffer.getCompany().getName(),
                jobOffer.getTitle(),
                jobOffer.getDescription(),
                jobOffer.getJobType(),
                jobOffer.getLocation(),
                jobOffer.getSalaryMin(),
                jobOffer.getSalaryMax(),
                jobOffer.getDeadline(),
                jobOffer.isActive(),
                jobOffer.getCreatedAt()
        );
    }
}

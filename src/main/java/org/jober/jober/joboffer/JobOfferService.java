
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyRepository companyRepository;

    public JobOfferService(
            JobOfferRepository jobOfferRepository,
            CompanyRepository companyRepository) {

        this.jobOfferRepository = jobOfferRepository;
        this.companyRepository = companyRepository;
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

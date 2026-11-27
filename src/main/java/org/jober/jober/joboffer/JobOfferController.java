
package org.jober.jober.joboffer;

import jakarta.validation.Valid;

import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.joboffer.dto.JobOfferCreateRequest;
import org.jober.jober.joboffer.dto.JobOfferUpdateRequest;
import org.jober.jober.joboffer.dto.JobOfferResponse;
import java.util.List;

import org.jober.jober.common.enums.JobType;
import org.jober.jober.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.jober.jober.joboffer.dto.JobOfferStats;

@RestController
@RequestMapping("/api/job-offers")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    public JobOfferController(JobOfferService jobOfferService) {
        this.jobOfferService = jobOfferService;
    }

@GetMapping
public PageResponse<JobOfferResponse> getAll(
        @RequestParam(required = false) JobType type,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Long companyId,
        @RequestParam(required = false) BigDecimal minSalary,
        @RequestParam(required = false) Boolean active,
        Pageable pageable) {

    return PageResponse.from(
            jobOfferService.getAll(
                    type,
                    location,
                    companyId,
                    minSalary,
                    active,
                    pageable
            )
    );
}

    // Get most popular job offers
@GetMapping("/popular")
public List<JobOfferStats> getPopularJobOffers(
        @RequestParam(name = "min", defaultValue = "1") long min) {

    return jobOfferService.getPopularJobOffers(min);
}


    // Get active job offers not yet applied to by current student
    @GetMapping("/not-applied")
    public PageResponse<JobOfferResponse> getNotAppliedJobOffers(
            CurrentUser user,
            Pageable pageable) {

        return PageResponse.from(
                jobOfferService.getNotAppliedJobOffers(
                        user,
                        pageable
                )
        );
    }

    // Get a job offer by ID
    @GetMapping("/{id}")
    public JobOfferResponse getById(
            @PathVariable Long id) {

        return jobOfferService.getById(id);
    }

    // Create a new job offer
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobOfferResponse create(
            @Valid @RequestBody JobOfferCreateRequest request,
            CurrentUser user) {

        return jobOfferService.create(request, user);
    }

    // Update an existing job offer
    @PutMapping("/{id}")
    public JobOfferResponse update(
            @PathVariable Long id,
            @Valid @RequestBody JobOfferUpdateRequest request,
            CurrentUser user) {

        return jobOfferService.update(id, request, user);
    }

    // Close a job offer
    @PatchMapping("/{id}/close")
    public JobOfferResponse close(
            @PathVariable Long id,
            CurrentUser user) {

        return jobOfferService.close(id, user);
    }

    // Delete a job offer
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            CurrentUser user) {

        jobOfferService.delete(id, user);
    }
}

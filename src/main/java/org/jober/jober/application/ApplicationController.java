package org.jober.jober.application;

import jakarta.validation.Valid;
import org.jober.jober.application.dto.ApplicationRequest;
import org.jober.jober.application.dto.ApplicationResponse;
import org.jober.jober.application.dto.StatusUpdateRequest;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/api/applications")
    public ApplicationResponse createApplication(
            CurrentUser currentUser,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return applicationService.createApplication(currentUser, request);
    }

    @GetMapping("/api/applications/me")
    public PageResponse<ApplicationResponse> getMyApplications(
            CurrentUser currentUser,
            Pageable pageable
    ) {
        return applicationService.getMyApplications(currentUser, pageable);
    }

    @PatchMapping("/api/applications/{id}/apply")
    public ApplicationResponse apply(
            CurrentUser currentUser,
            @PathVariable Long id
    ) {
        return applicationService.applyToApplication(currentUser, id);
    }

    @PatchMapping("/api/applications/{id}/withdraw")
    public ApplicationResponse withdraw(
            CurrentUser currentUser,
            @PathVariable Long id
    ) {
        return applicationService.withdrawApplication(currentUser, id);
    }

    @GetMapping("/api/company/applications")
    public PageResponse<ApplicationResponse> getCompanyApplications(
            CurrentUser currentUser,
            Pageable pageable
    ) {
        return applicationService.getCompanyApplications(currentUser, pageable);
    }

    @PatchMapping("/api/applications/{id}/status")
    public ApplicationResponse updateStatus(
            CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return applicationService.updateStatusByCompany(currentUser, id, request);
    }
}

package org.jober.jober.interview;

import jakarta.validation.Valid;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.interview.dto.InterviewRequest;
import org.jober.jober.interview.dto.InterviewResponse;
import org.jober.jober.interview.dto.OutcomeUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse createInterview(CurrentUser currentUser,
                                             @Valid @RequestBody InterviewRequest request) {
        return interviewService.createInterview(currentUser, request);
    }

    @PatchMapping("/{id}/outcome")
    public InterviewResponse updateOutcome(CurrentUser currentUser,
                                           @PathVariable Long id,
                                           @Valid @RequestBody OutcomeUpdateRequest request) {
        return interviewService.updateOutcome(currentUser, id, request);
    }

    @GetMapping("/application/{applicationId}")
    public List<InterviewResponse> getByApplication(CurrentUser currentUser,
                                                    @PathVariable Long applicationId) {
        return interviewService.getInterviewsByApplication(currentUser, applicationId);
    }
}
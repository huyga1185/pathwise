package uk.huy.pathwise.job.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.job.dto.JobResponse;
import uk.huy.pathwise.job.service.JobService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

/**
 * Jobs of the caller's store (API_SPEC §6), for clients polling a long operation.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobs")
@PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
public class JobController {
    private final JobService jobService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@AuthenticationPrincipal AuthenticatedUser actor,
                                                           @PathVariable("id") long jobId) {
        return ResponseEntity.ok(new ApiResponse<>(jobService.getJob(actor.identity(), jobId)));
    }
}

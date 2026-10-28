package uk.huy.pathwise.planning.controller;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.job.dto.JobResponse;
import uk.huy.pathwise.planning.dto.request.PatchStopRequest;
import uk.huy.pathwise.planning.dto.request.PlanDraftRequest;
import uk.huy.pathwise.planning.dto.request.PlanVersionRequest;
import uk.huy.pathwise.planning.dto.response.PlanDetailResponse;
import uk.huy.pathwise.planning.dto.response.PlanSummaryResponse;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.service.PlanService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.time.LocalDate;

/**
 * Plan drafts of the caller's store (API_SPEC §7). OWNER and DISPATCHER only (ARCHITECTURE.md §6.3).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plans")
@PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
public class PlanController {
    private final PlanService planService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlanDetailResponse>> createPlan(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                      @Valid @RequestBody PlanDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(planService.createPlan(actor.identity(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlanSummaryResponse>>> listPlans(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) PlanStatus status,
            @PageableDefault(sort = "planDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(planService.listPlans(actor.identity(), date, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDetailResponse>> getPlan(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                   @PathVariable("id") long planId) {
        return ResponseEntity.ok(new ApiResponse<>(planService.getPlan(actor.identity(), planId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDetailResponse>> replacePlan(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("id") long planId,
            @Validated({Default.class, PlanDraftRequest.Replace.class}) @RequestBody PlanDraftRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(planService.replacePlan(actor.identity(), planId, request)));
    }

    /**
     * Merge patch: an absent field keeps its value, {@code null} clears it ({@link PatchStopRequest}).
     */
    @PatchMapping("/{id}/stops/{stopId}")
    public ResponseEntity<ApiResponse<PlanDetailResponse>> patchStop(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                     @PathVariable("id") long planId,
                                                                     @PathVariable("stopId") long stopId,
                                                                     @Valid @RequestBody PatchStopRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(planService.patchStop(actor.identity(), planId, stopId, request)));
    }

    /**
     * Geocodes the PENDING and FAILED stops again; 202 with the job (clients poll it or the plan).
     */
    @PostMapping("/{id}/geocode")
    public ResponseEntity<ApiResponse<JobResponse>> geocodePlan(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                @PathVariable("id") long planId,
                                                                @Valid @RequestBody PlanVersionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(planService.geocodePlan(actor.identity(), planId, request)));
    }

    /**
     * Optimizes the plan; 202 with the job. Inline by default, so the job is usually finished when this returns.
     */
    @PostMapping("/{id}/optimize")
    public ResponseEntity<ApiResponse<JobResponse>> optimizePlan(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                 @PathVariable("id") long planId,
                                                                 @Valid @RequestBody PlanVersionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(planService.optimizePlan(actor.identity(), planId, request)));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<PlanDetailResponse>> publishPlan(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                       @PathVariable("id") long planId,
                                                                       @Valid @RequestBody PlanVersionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(planService.publishPlan(actor.identity(), planId, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PlanDetailResponse>> cancelPlan(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                      @PathVariable("id") long planId,
                                                                      @Valid @RequestBody PlanVersionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(planService.cancelPlan(actor.identity(), planId, request)));
    }
}

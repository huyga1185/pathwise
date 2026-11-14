package uk.huy.pathwise.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.admin.dto.response.BusinessMetricsResponse;
import uk.huy.pathwise.admin.dto.response.TechnicalMetricsResponse;
import uk.huy.pathwise.admin.service.AdminMetricsService;
import uk.huy.pathwise.core.apiresponse.ApiResponse;

import java.time.LocalDate;

/**
 * Platform numbers for the ADMIN overview (T60, FR-A16).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricsController {
    private final AdminMetricsService adminMetricsService;

    /**
     * @param from first day (Asia/Ho_Chi_Minh), inclusive; default 29 days before {@code to}
     * @param to   last day, inclusive; default today
     */
    @GetMapping("/business")
    public ResponseEntity<ApiResponse<BusinessMetricsResponse>> business(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(new ApiResponse<>(adminMetricsService.business(from, to)));
    }

    @GetMapping("/technical")
    public ResponseEntity<ApiResponse<TechnicalMetricsResponse>> technical() {
        return ResponseEntity.ok(new ApiResponse<>(adminMetricsService.technical()));
    }
}

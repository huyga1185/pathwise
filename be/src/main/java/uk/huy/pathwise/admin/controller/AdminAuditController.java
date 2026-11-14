package uk.huy.pathwise.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.admin.dto.response.AuditEventResponse;
import uk.huy.pathwise.admin.service.AdminAuditService;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.core.pagination.PageResponse;

import java.time.LocalDate;

/**
 * The audit log of the whole platform, for the ADMIN (T58, FR-A14).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AdminAuditService adminAuditService;

    /**
     * @param from first day (Asia/Ho_Chi_Minh), inclusive
     * @param to   last day (Asia/Ho_Chi_Minh), inclusive
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditEventResponse>>> listEvents(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(adminAuditService.listEvents(storeId, actorId, action, from, to, pageable)));
    }
}

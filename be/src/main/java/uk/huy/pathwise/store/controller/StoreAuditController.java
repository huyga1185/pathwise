package uk.huy.pathwise.store.controller;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.store.dto.response.StoreAuditEventResponse;
import uk.huy.pathwise.store.service.StoreAuditService;

import java.time.LocalDate;

/**
 * Audit log of the caller's store, for its OWNER (T58, FR-A14).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/store/audit")
public class StoreAuditController {
    private final StoreAuditService storeAuditService;

    /**
     * @param from first day (store time zone), inclusive
     * @param to   last day (store time zone), inclusive
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<StoreAuditEventResponse>>> listEvents(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(
                storeAuditService.listEvents(actor.identity(), actorId, action, from, to, pageable)));
    }
}

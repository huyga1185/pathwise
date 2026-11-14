package uk.huy.pathwise.planning.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse;
import uk.huy.pathwise.planning.service.StoreReportService;
import uk.huy.pathwise.planning.service.StoreReportService.Period;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Reports of the caller's store (API_SPEC §7a, T63). OWNER and DISPATCHER only. Reading is allowed while the store is
 * locked, like every GET (P-62).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
public class ReportController {
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final StoreReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StoreReportResponse>> summary(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to) {
        Period period = reportService.period(actor.identity(), from, to);
        return ResponseEntity.ok(new ApiResponse<>(reportService.summary(period)));
    }

    /**
     * Streams the file; the period is checked before anything is written, so a bad period is still a JSON error.
     */
    @GetMapping(value = "/stops.csv", produces = "text/csv")
    public void stopsCsv(@AuthenticationPrincipal AuthenticatedUser actor,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
                         HttpServletResponse response) throws IOException {
        Period period = reportService.period(actor.identity(), from, to);
        String filename = "pathwise-" + period.from().format(FILE_DATE) + "-" + period.to().format(FILE_DATE) + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString());
        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        reportService.writeStopsCsv(period, writer);
    }
}

package uk.huy.pathwise.planning.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.planning.dto.response.PlanImportResponse;
import uk.huy.pathwise.planning.service.PlanImportService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Stop import from a file (API_SPEC §7, T61): read only, the stops are saved with the plan as usual. OWNER and
 * DISPATCHER only.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plans/import")
@PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
public class PlanImportController {
    static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final PlanImportService planImportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PlanImportResponse>> importStops(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                       @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(new ApiResponse<>(
                planImportService.importStops(actor.identity(), file.getOriginalFilename(), file.getBytes())));
    }

    @GetMapping(value = "/template.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> xlsxTemplate() {
        return download(planImportService.xlsxTemplate(), XLSX, "pathwise-mau-diem-giao.xlsx");
    }

    @GetMapping(value = "/template.csv", produces = "text/csv")
    public ResponseEntity<byte[]> csvTemplate() {
        return download(planImportService.csvTemplate(), CSV, "pathwise-mau-diem-giao.csv");
    }

    private static ResponseEntity<byte[]> download(byte[] content, MediaType type, String filename) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}

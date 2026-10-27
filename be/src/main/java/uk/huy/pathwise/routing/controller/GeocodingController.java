package uk.huy.pathwise.routing.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.routing.dto.response.PlaceResponse;
import uk.huy.pathwise.routing.service.GeocodingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/geocoding")
public class GeocodingController {
    private final GeocodingService geocodingService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> search(
            @RequestParam("q") @NotBlank(message = "QUERY_REQUIRED") @Size(min = 3, max = 300, message = "INVALID_QUERY_LENGTH") String query,
            @RequestParam(value = "limit", defaultValue = "5") @Min(value = 1, message = "INVALID_LIMIT") @Max(value = 10, message = "INVALID_LIMIT") int limit) {
        return ResponseEntity.ok(new ApiResponse<>(geocodingService.search(query.trim(), limit)));
    }
}

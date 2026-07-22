package uk.huy.pathwise.auth.controller;

import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.auth.infastructure.annotation.CurrentClient;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.service.AuthService;
import uk.huy.pathwise.shared.response.ApiResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/log-in")
    public ResponseEntity<ApiResponse<LogInResponse>> logIn(@CurrentClient ClientDetail clientDetail,
                                                           @RequestBody LogInRequest request) throws JOSEException {
        return ResponseEntity.ok().body(new ApiResponse<>(null,
                authService.logIn(clientDetail, request)));
    }
}

package org.open4goods.nudgerfrontapi.controller;

import org.open4goods.nudgerfrontapi.dto.AuthRequest;
import org.open4goods.nudgerfrontapi.dto.AuthResponse;
import org.open4goods.nudgerfrontapi.dto.RefreshRequest;
import org.open4goods.nudgerfrontapi.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/token")
    @Operation(summary = "Issue an access token")
    public ResponseEntity<AuthResponse> token(@RequestBody AuthRequest request) {
        String token = authService.token(request.username(), request.password());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "Refresh an access token")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        String token = authService.refresh(request.refreshToken());
        if (token == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(new AuthResponse(token));
    }
}

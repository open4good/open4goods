package org.open4goods.nudgerfrontapi.controller.auth;

import org.open4goods.nudgerfrontapi.dto.auth.AuthTokensDto;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Authentication endpoints for the frontend.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login and refresh tokens")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login with XWiki credentials",
            description = "Validate credentials against XWiki and return JWT tokens as cookies.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication success",
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication failed")
            }
    )
    public ResponseEntity<AuthTokensDto> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            String access = jwtService.generateAccessToken(auth);
            String refresh = jwtService.generateRefreshToken(auth);

            ResponseCookie accessCookie = ResponseCookie.from("access-token", access)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(jwtService.getProperties().getAccessTokenExpiry())
                    .build();
            ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", refresh)
                    .httpOnly(true)
                    .path("/auth/refresh")
                    .maxAge(jwtService.getProperties().getRefreshTokenExpiry())
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(new AuthTokensDto(access, refresh));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Issue a new access token using the refresh token cookie.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed",
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
            }
    )
    public ResponseEntity<AuthTokensDto> refresh(@jakarta.servlet.http.CookieValue("refresh-token") String refreshToken) {
        try {
            String user = jwtService.validateRefreshToken(refreshToken);
            Authentication auth = new UsernamePasswordAuthenticationToken(user, "N/A");
            String access = jwtService.generateAccessToken(auth);
            ResponseCookie accessCookie = ResponseCookie.from("access-token", access)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(jwtService.getProperties().getAccessTokenExpiry())
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(new AuthTokensDto(access, refreshToken));
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
        }
    }
}

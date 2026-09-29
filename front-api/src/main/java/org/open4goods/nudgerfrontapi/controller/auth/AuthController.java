package org.open4goods.nudgerfrontapi.controller.auth;

import java.time.Duration;

import org.open4goods.nudgerfrontapi.dto.auth.AuthTokensDto;
import org.open4goods.nudgerfrontapi.dto.auth.GoogleLoginRequest;
import org.open4goods.nudgerfrontapi.dto.auth.LogoutResponse;
import org.open4goods.nudgerfrontapi.service.auth.GoogleIdentityService;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
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

    private final JwtService jwtService;
    private final GoogleIdentityService googleIdentityService;

    public AuthController(JwtService jwtService, GoogleIdentityService googleIdentityService) {
        this.jwtService = jwtService;
        this.googleIdentityService = googleIdentityService;
    }

    @PostMapping("/google")
    @Operation(
            summary = "Create an application session from a verified Google identity",
            description = "Accept an ID token only from the Nuxt BFF and return application JWTs.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = GoogleLoginRequest.class))
            ),

            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication success",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication failed")
            }
    )
    public ResponseEntity<AuthTokensDto> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            GoogleIdentityService.VerifiedIdentity identity = googleIdentityService.verify(request.idToken());
            Authentication auth = new UsernamePasswordAuthenticationToken(identity.email(), "N/A",
                    identity.roles().stream().map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList());
            String access = jwtService.generateAccessToken(auth);
            String refresh = jwtService.generateRefreshToken(auth);


            return ResponseEntity.ok()
                    .body(new AuthTokensDto(access, refresh));
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Issue a new access token using the refresh token cookie.",

            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
            }
    )
    public ResponseEntity<AuthTokensDto> refresh(
            @CookieValue("${front.security.refresh-token-cookie-name:refresh_token}") String refreshToken) {
        try {
            GoogleIdentityService.VerifiedIdentity identity = googleIdentityService.identityFor(jwtService.validateRefreshToken(refreshToken));
            Authentication auth = new UsernamePasswordAuthenticationToken(identity.email(), "N/A",
                    identity.roles().stream().map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList());
            String access = jwtService.generateAccessToken(auth);
            String newRefresh = jwtService.generateRefreshToken(auth);


            return ResponseEntity.ok()
                    .body(new AuthTokensDto(access, newRefresh));
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Expire the access and refresh token cookies so the session becomes invalid.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logout successful",
                            content = @Content(schema = @Schema(implementation = LogoutResponse.class)))
            }
    )
    public ResponseEntity<LogoutResponse> logout() {
        ResponseCookie clearAccessToken = ResponseCookie.from("access-token", "")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .path("/")
                .build();
        ResponseCookie clearRefreshToken = ResponseCookie.from("refresh-token", "")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessToken.toString(), clearRefreshToken.toString())
                .body(new LogoutResponse(true));
    }
}

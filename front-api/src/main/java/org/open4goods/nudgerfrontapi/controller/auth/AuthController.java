package org.open4goods.nudgerfrontapi.controller.auth;

import java.time.Duration;
import java.util.List;

import org.open4goods.nudgerfrontapi.dto.auth.AuthTokensDto;
import org.open4goods.nudgerfrontapi.dto.auth.GoogleSsoLoginRequest;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.nudgerfrontapi.dto.auth.LogoutResponse;
import org.open4goods.nudgerfrontapi.service.auth.GoogleIdentity;
import org.open4goods.nudgerfrontapi.service.auth.GoogleIdentityService;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.open4goods.nudgerfrontapi.service.exception.GoogleIdentityVerificationException;
import org.open4goods.nudgerfrontapi.service.exception.GoogleSsoDisabledException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleIdentityService googleIdentityService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            GoogleIdentityService googleIdentityService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.googleIdentityService = googleIdentityService;
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
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
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


            return ResponseEntity.ok()
                    .body(new AuthTokensDto(access, refresh));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/google")
    @Operation(
            summary = "Login with Google SSO",
            description = "Verify a Google ID token forwarded by the BFF (signature, issuer, audience, expiry, "
                    + "nonce and verified email) and issue JWT tokens as cookies when the resolved email is present "
                    + "in the environment role allowlist. Disabled by default.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = GoogleSsoLoginRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication success",
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Token verification failed or email not allowlisted"),
                    @ApiResponse(responseCode = "404", description = "Google SSO is disabled")
            }
    )
    public ResponseEntity<AuthTokensDto> loginWithGoogle(@RequestBody GoogleSsoLoginRequest request) {
        try {
            GoogleIdentity identity = googleIdentityService.verify(request.idToken(), request.nonce());
            List<GrantedAuthority> authorities = identity.roles().stream()
                    .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
            Authentication auth = new UsernamePasswordAuthenticationToken(identity.email(), "N/A", authorities);

            String access = jwtService.generateAccessToken(auth);
            String refresh = jwtService.generateRefreshToken(auth);

            ResponseCookie accessTokenCookie = buildCookie("access-token", access,
                    jwtService.getProperties().getAccessTokenExpiry());
            ResponseCookie refreshTokenCookie = buildCookie("refresh-token", refresh,
                    jwtService.getProperties().getRefreshTokenExpiry());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString(), refreshTokenCookie.toString())
                    .body(new AuthTokensDto(access, refresh));
        } catch (GoogleSsoDisabledException ex) {
            return ResponseEntity.notFound().build();
        } catch (GoogleIdentityVerificationException ex) {
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
    public ResponseEntity<AuthTokensDto> refresh(@CookieValue("refresh-token") String refreshToken                                                ) {
        try {
            Authentication auth = jwtService.validateRefreshToken(refreshToken);
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

package org.open4goods.nudgerfrontapi.controller.auth;

import org.open4goods.nudgerfrontapi.dto.auth.AuthTokensDto;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,
                            required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication success",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication failed")
            }
    )
    public ResponseEntity<AuthTokensDto> login(@RequestBody LoginRequest request,
                                               @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            String access = jwtService.generateAccessToken(auth, domainLanguage);
            String refresh = jwtService.generateRefreshToken(auth, domainLanguage);


            return ResponseEntity.ok()
                    .body(new AuthTokensDto(access, refresh));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Issue a new access token using the refresh token cookie.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,
                            required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(schema = @Schema(implementation = AuthTokensDto.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
            }
    )
    public ResponseEntity<AuthTokensDto> refresh(@CookieValue("refresh-token") String refreshToken,
                                                 @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        try {
            String user = jwtService.validateRefreshToken(refreshToken, domainLanguage);
            Authentication auth = new UsernamePasswordAuthenticationToken(user, "N/A");
            String access = jwtService.generateAccessToken(auth, domainLanguage);
            String newRefresh = jwtService.generateRefreshToken(auth, domainLanguage);


            return ResponseEntity.ok()
                    .body(new AuthTokensDto(access, newRefresh));
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
        }
    }
}

package org.open4goods.b2bapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.open4goods.b2bapi.dto.AuthResponse;
import org.open4goods.b2bapi.dto.OidcLoginRequest;
import org.open4goods.b2bapi.service.AuthService;
import org.open4goods.b2bapi.service.AuthTokenResolver;
import org.open4goods.b2bapi.service.DashboardSessionService;
import org.open4goods.b2bapi.service.JwtCookieService;
import org.open4goods.b2bapi.service.JwtTokenPair;
import org.open4goods.b2bapi.service.ProvisionedAccount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard authentication endpoints for OIDC login and JWT session handling.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnBean(name = "entityManagerFactory")
public class AuthController {

    private final AuthService authService;
    private final DashboardSessionService dashboardSessionService;
    private final AuthTokenResolver authTokenResolver;
    private final JwtCookieService jwtCookieService;

    public AuthController(
            final AuthService authService,
            final DashboardSessionService dashboardSessionService,
            final AuthTokenResolver authTokenResolver,
            final JwtCookieService jwtCookieService) {
        this.authService = authService;
        this.dashboardSessionService = dashboardSessionService;
        this.authTokenResolver = authTokenResolver;
        this.jwtCookieService = jwtCookieService;
    }

    /**
     * Verifies an OIDC/OAuth provider token, provisions the user, and starts a dashboard session.
     *
     * @param request login request
     * @param response servlet response for HttpOnly cookie writes
     * @return authenticated session payload
     */
    @PostMapping("/oidc")
    public AuthResponse login(@Valid @RequestBody final OidcLoginRequest request,
            final HttpServletResponse response) {
        final AuthResponse authResponse = authService.login(request.provider(), request.idToken());
        jwtCookieService.writeSessionCookies(response, new JwtTokenPair(
                authResponse.accessToken(),
                authResponse.accessExpiresAt(),
                authResponse.refreshToken(),
                authResponse.refreshExpiresAt()));
        return authResponse;
    }

    /**
     * Rotates dashboard JWTs from a valid refresh token.
     *
     * @param request servlet request with refresh cookie or bearer token
     * @param response servlet response for refreshed HttpOnly cookie writes
     * @return refreshed session payload
     */
    @PostMapping("/refresh")
    public AuthResponse refresh(final HttpServletRequest request, final HttpServletResponse response) {
        final AuthResponse authResponse = dashboardSessionService.refresh(authTokenResolver.resolveRefreshToken(request));
        jwtCookieService.writeSessionCookies(response, new JwtTokenPair(
                authResponse.accessToken(),
                authResponse.accessExpiresAt(),
                authResponse.refreshToken(),
                authResponse.refreshExpiresAt()));
        return authResponse;
    }

    /**
     * Clears dashboard session cookies.
     *
     * @param response servlet response for expired cookie writes
     */
    @PostMapping("/logout")
    public void logout(final HttpServletResponse response) {
        jwtCookieService.clearSessionCookies(response);
    }

    /**
     * Returns the current dashboard session state from an access token.
     *
     * @param request servlet request with access cookie or bearer token
     * @return current account payload without rotating tokens
     */
    @GetMapping("/me")
    public AuthResponse me(final HttpServletRequest request) {
        final ProvisionedAccount account = dashboardSessionService.currentAccount(
                authTokenResolver.resolveAccessToken(request));
        return dashboardSessionService.toAuthResponse(account, new JwtTokenPair(null, null, null, null));
    }
}

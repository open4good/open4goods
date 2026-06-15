package org.open4goods.b2bapi.service;

import org.open4goods.b2bapi.dto.AuthResponse;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Coordinates provider verification, provisioning, and JWT issuance.
 */
@Service
@ConditionalOnBean(name = "entityManagerFactory")
public class AuthService {

    private final OidcVerifierService oidcVerifierService;
    private final UserProvisioningService userProvisioningService;
    private final JwtTokenService jwtTokenService;
    private final DashboardSessionService dashboardSessionService;

    public AuthService(
            final OidcVerifierService oidcVerifierService,
            final UserProvisioningService userProvisioningService,
            final JwtTokenService jwtTokenService,
            final DashboardSessionService dashboardSessionService) {
        this.oidcVerifierService = oidcVerifierService;
        this.userProvisioningService = userProvisioningService;
        this.jwtTokenService = jwtTokenService;
        this.dashboardSessionService = dashboardSessionService;
    }

    /**
     * Logs in a dashboard user with a provider token.
     *
     * @param provider OIDC/OAuth provider
     * @param token provider token
     * @return auth response including JWTs and account state
     */
    public AuthResponse login(final OidcProvider provider, final String token) {
        final OidcUserProfile profile = oidcVerifierService.verify(provider, token);
        final ProvisionedAccount account = userProvisioningService.provision(profile);
        final JwtTokenPair tokens = jwtTokenService.issueTokenPair(account.user(), account.organization());
        return dashboardSessionService.toAuthResponse(account, tokens);
    }
}

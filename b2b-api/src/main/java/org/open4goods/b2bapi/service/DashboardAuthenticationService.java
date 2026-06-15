package org.open4goods.b2bapi.service;

import java.util.ArrayList;
import java.util.List;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Converts access JWTs into Spring Security dashboard authentications.
 */
@Service
@ConditionalOnBean(name = "entityManagerFactory")
public class DashboardAuthenticationService {

    private final DashboardSessionService dashboardSessionService;

    public DashboardAuthenticationService(final DashboardSessionService dashboardSessionService) {
        this.dashboardSessionService = dashboardSessionService;
    }

    /**
     * Verifies an access token and returns a populated Spring Security authentication.
     *
     * @param accessToken dashboard access JWT
     * @return authenticated dashboard principal
     */
    public Authentication authenticate(final String accessToken) {
        final ProvisionedAccount account = dashboardSessionService.currentAccount(accessToken);
        final DashboardPrincipal principal = new DashboardPrincipal(
                account.user().getId(),
                account.organization().getId(),
                account.user().getEmail(),
                account.user().isPlatformAdmin(),
                account.role());
        return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities(principal));
    }

    private List<SimpleGrantedAuthority> authorities(final DashboardPrincipal principal) {
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(RbacAuthority.DASHBOARD));
        authorities.add(new SimpleGrantedAuthority(roleAuthority(principal.role())));
        if (principal.platformAdmin()) {
            authorities.add(new SimpleGrantedAuthority(RbacAuthority.PLATFORM_ADMIN));
        }
        return authorities;
    }

    private String roleAuthority(final OrganizationRole role) {
        return switch (role) {
            case OWNER -> RbacAuthority.ORG_OWNER;
            case ADMIN -> RbacAuthority.ORG_ADMIN;
            case DEVELOPER -> RbacAuthority.ORG_DEVELOPER;
            case BILLING -> RbacAuthority.ORG_BILLING;
        };
    }
}

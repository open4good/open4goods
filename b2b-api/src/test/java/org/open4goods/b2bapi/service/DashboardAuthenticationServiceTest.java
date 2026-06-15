package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;

/**
 * Unit tests for dashboard JWT authority mapping.
 */
class DashboardAuthenticationServiceTest {

    @Test
    void authenticatesDashboardPrincipalWithRoleAndPlatformAuthorities() {
        final DashboardSessionService sessionService = org.mockito.Mockito.mock(DashboardSessionService.class);
        final DashboardAuthenticationService service = new DashboardAuthenticationService(sessionService);
        final User user = new User("admin@example.com", OidcProvider.GOOGLE, "subject");
        user.setPlatformAdmin(true);
        final Organization organization = new Organization("Admin workspace", "admin-workspace");
        when(sessionService.currentAccount("access-token")).thenReturn(
                new ProvisionedAccount(user, organization, OrganizationRole.ADMIN, 2500L));

        final var authentication = service.authenticate("access-token");

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new DashboardPrincipal(
                user.getId(),
                organization.getId(),
                "admin@example.com",
                true,
                OrganizationRole.ADMIN));
        assertThat(authorityNames(authentication)).contains(
                RbacAuthority.DASHBOARD,
                RbacAuthority.ORG_ADMIN,
                RbacAuthority.PLATFORM_ADMIN);
    }

    @Test
    void nonPlatformDeveloperDoesNotReceivePlatformAuthority() {
        final DashboardSessionService sessionService = org.mockito.Mockito.mock(DashboardSessionService.class);
        final DashboardAuthenticationService service = new DashboardAuthenticationService(sessionService);
        final User user = new User("developer@example.com", OidcProvider.GITHUB, "subject");
        final Organization organization = new Organization("Developer workspace", "developer-workspace");
        when(sessionService.currentAccount("access-token")).thenReturn(
                new ProvisionedAccount(user, organization, OrganizationRole.DEVELOPER, 0L));

        final var authentication = service.authenticate("access-token");

        assertThat(authorityNames(authentication)).contains(
                RbacAuthority.DASHBOARD,
                RbacAuthority.ORG_DEVELOPER);
        assertThat(authorityNames(authentication)).doesNotContain(RbacAuthority.PLATFORM_ADMIN);
    }

    private java.util.List<String> authorityNames(final org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList();
    }
}

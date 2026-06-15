package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Unit tests for the organization role permission matrix.
 */
class OrganizationRbacServiceTest {

    private final OrganizationRbacService service = new OrganizationRbacService();

    @Test
    void ownerHasAllOrganizationPermissions() {
        final UUID userId = UUID.randomUUID();
        final Authentication authentication = authentication(userId, OrganizationRole.OWNER, false);

        assertThat(service.canTransferOwnership(authentication)).isTrue();
        assertThat(service.canManageMembers(authentication)).isTrue();
        assertThat(service.canManageAnyApiKey(authentication)).isTrue();
        assertThat(service.canManageOwnApiKey(authentication, UUID.randomUUID())).isTrue();
        assertThat(service.canUsePlaygroundOrReadUsage(authentication)).isTrue();
        assertThat(service.canManageBilling(authentication)).isTrue();
        assertThat(service.canReadBilling(authentication)).isTrue();
    }

    @Test
    void developerCanOnlyManageOwnKeysAndReadUsage() {
        final UUID userId = UUID.randomUUID();
        final Authentication authentication = authentication(userId, OrganizationRole.DEVELOPER, false);

        assertThat(service.canTransferOwnership(authentication)).isFalse();
        assertThat(service.canManageMembers(authentication)).isFalse();
        assertThat(service.canManageAnyApiKey(authentication)).isFalse();
        assertThat(service.canManageOwnApiKey(authentication, userId)).isTrue();
        assertThat(service.canManageOwnApiKey(authentication, UUID.randomUUID())).isFalse();
        assertThat(service.canUsePlaygroundOrReadUsage(authentication)).isTrue();
        assertThat(service.canManageBilling(authentication)).isFalse();
        assertThat(service.canReadBilling(authentication)).isTrue();
    }

    @Test
    void billingCanManageAndReadBillingOnly() {
        final Authentication authentication = authentication(UUID.randomUUID(), OrganizationRole.BILLING, false);

        assertThat(service.canTransferOwnership(authentication)).isFalse();
        assertThat(service.canManageMembers(authentication)).isFalse();
        assertThat(service.canManageAnyApiKey(authentication)).isFalse();
        assertThat(service.canUsePlaygroundOrReadUsage(authentication)).isFalse();
        assertThat(service.canManageBilling(authentication)).isTrue();
        assertThat(service.canReadBilling(authentication)).isTrue();
    }

    @Test
    void platformAdminIsSeparateFromOrganizationRole() {
        final Authentication authentication = authentication(UUID.randomUUID(), OrganizationRole.DEVELOPER, true);

        assertThat(service.isPlatformAdmin(authentication)).isTrue();
        assertThat(service.canManageMembers(authentication)).isFalse();
    }

    private Authentication authentication(final UUID userId, final OrganizationRole role, final boolean platformAdmin) {
        final DashboardPrincipal principal = new DashboardPrincipal(
                userId,
                UUID.randomUUID(),
                "user@example.com",
                platformAdmin,
                role);
        return new UsernamePasswordAuthenticationToken(principal, "token");
    }
}

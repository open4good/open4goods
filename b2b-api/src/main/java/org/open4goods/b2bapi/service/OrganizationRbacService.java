package org.open4goods.b2bapi.service;

import java.util.UUID;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Organization and platform authorization matrix for use from {@code @PreAuthorize}.
 */
@Service("organizationRbacService")
public class OrganizationRbacService {

    public boolean canTransferOwnership(final Authentication authentication) {
        return role(authentication) == OrganizationRole.OWNER;
    }

    public boolean canManageMembers(final Authentication authentication) {
        final OrganizationRole role = role(authentication);
        return role == OrganizationRole.OWNER || role == OrganizationRole.ADMIN;
    }

    public boolean canManageAnyApiKey(final Authentication authentication) {
        final OrganizationRole role = role(authentication);
        return role == OrganizationRole.OWNER || role == OrganizationRole.ADMIN;
    }

    public boolean canManageOwnApiKey(final Authentication authentication, final UUID createdBy) {
        final DashboardPrincipal principal = principal(authentication);
        if (principal == null) {
            return false;
        }
        return canManageAnyApiKey(authentication)
                || (principal.role() == OrganizationRole.DEVELOPER && principal.userId().equals(createdBy));
    }

    public boolean canUsePlaygroundOrReadUsage(final Authentication authentication) {
        final OrganizationRole role = role(authentication);
        return role == OrganizationRole.OWNER || role == OrganizationRole.ADMIN || role == OrganizationRole.DEVELOPER;
    }

    public boolean canManageBilling(final Authentication authentication) {
        final OrganizationRole role = role(authentication);
        return role == OrganizationRole.OWNER || role == OrganizationRole.ADMIN || role == OrganizationRole.BILLING;
    }

    public boolean canReadBilling(final Authentication authentication) {
        return role(authentication) != null;
    }

    public boolean isPlatformAdmin(final Authentication authentication) {
        final DashboardPrincipal principal = principal(authentication);
        return principal != null && principal.platformAdmin();
    }

    private OrganizationRole role(final Authentication authentication) {
        final DashboardPrincipal principal = principal(authentication);
        return principal == null ? null : principal.role();
    }

    private DashboardPrincipal principal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardPrincipal principal)) {
            return null;
        }
        return principal;
    }
}

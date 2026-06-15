package org.open4goods.b2bapi.service;

import java.util.UUID;
import org.open4goods.b2bapi.model.OrganizationRole;

/**
 * Authenticated dashboard principal carried by Spring Security.
 *
 * @param userId authenticated user id
 * @param organizationId active organization id
 * @param email authenticated email
 * @param platformAdmin true when the user can access platform admin endpoints
 * @param role role in the active organization
 */
public record DashboardPrincipal(
        UUID userId,
        UUID organizationId,
        String email,
        boolean platformAdmin,
        OrganizationRole role) {
}

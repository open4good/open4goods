package org.open4goods.b2bapi.service;

import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;

/**
 * Account state after OIDC provisioning or session loading.
 *
 * @param user dashboard user
 * @param organization active organization
 * @param role user's role in the active organization
 * @param balanceCredits authoritative current live credit balance
 */
public record ProvisionedAccount(
        User user,
        Organization organization,
        OrganizationRole role,
        long balanceCredits) {
}

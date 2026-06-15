package org.open4goods.b2bapi.service;

import org.open4goods.b2bapi.model.OidcProvider;

/**
 * Normalized identity profile returned by an external auth provider.
 */
public record OidcUserProfile(
        OidcProvider provider,
        String subject,
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl) {
}

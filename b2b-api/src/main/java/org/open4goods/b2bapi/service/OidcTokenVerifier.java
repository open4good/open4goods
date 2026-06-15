package org.open4goods.b2bapi.service;

import org.open4goods.b2bapi.model.OidcProvider;

/**
 * Verifies an external identity-provider credential.
 */
public interface OidcTokenVerifier {

    OidcProvider provider();

    OidcUserProfile verify(String credential);
}

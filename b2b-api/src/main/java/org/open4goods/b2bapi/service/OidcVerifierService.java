package org.open4goods.b2bapi.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.stereotype.Service;

/**
 * Routes provider credentials to the matching verifier.
 */
@Service
public class OidcVerifierService {

    private final Map<OidcProvider, OidcTokenVerifier> verifiers;

    public OidcVerifierService(final List<OidcTokenVerifier> verifiers) {
        this.verifiers = new EnumMap<>(OidcProvider.class);
        verifiers.forEach(verifier -> this.verifiers.put(verifier.provider(), verifier));
    }

    /**
     * Verifies an external provider credential.
     *
     * @param provider provider identifier
     * @param credential ID token or OAuth access token
     * @return normalized user profile
     */
    public OidcUserProfile verify(final OidcProvider provider, final String credential) {
        final OidcTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new InvalidCredentialsException("Unsupported OIDC provider: " + provider);
        }
        return verifier.verify(credential);
    }
}

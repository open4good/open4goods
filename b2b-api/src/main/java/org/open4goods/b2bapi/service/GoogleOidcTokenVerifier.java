package org.open4goods.b2bapi.service;

import java.time.Clock;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.stereotype.Service;

/**
 * Verifies Google OIDC ID tokens.
 */
@Service
public class GoogleOidcTokenVerifier extends JwksOidcTokenVerifier {

    public GoogleOidcTokenVerifier(final B2bApiProperties properties) {
        super(OidcProvider.GOOGLE, properties.getSecurity().getOidc().getGoogle(), Clock.systemUTC());
    }
}

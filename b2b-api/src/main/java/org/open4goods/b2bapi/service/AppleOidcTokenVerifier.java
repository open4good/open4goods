package org.open4goods.b2bapi.service;

import java.time.Clock;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.stereotype.Service;

/**
 * Verifies Apple OIDC ID tokens.
 */
@Service
public class AppleOidcTokenVerifier extends JwksOidcTokenVerifier {

    public AppleOidcTokenVerifier(final B2bApiProperties properties) {
        super(OidcProvider.APPLE, properties.getSecurity().getOidc().getApple(), Clock.systemUTC());
    }
}

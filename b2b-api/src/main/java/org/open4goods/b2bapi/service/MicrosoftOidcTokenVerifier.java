package org.open4goods.b2bapi.service;

import java.time.Clock;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.stereotype.Service;

/**
 * Verifies Microsoft OIDC ID tokens.
 */
@Service
public class MicrosoftOidcTokenVerifier extends JwksOidcTokenVerifier {

    public MicrosoftOidcTokenVerifier(final B2bApiProperties properties) {
        super(OidcProvider.MICROSOFT, properties.getSecurity().getOidc().getMicrosoft(), Clock.systemUTC());
    }

    @Override
    protected boolean issuerMatches(final String tokenIssuer) {
        final String configuredIssuer = oidcProperties().getIssuer();
        if (configuredIssuer.contains("/common/")) {
            return tokenIssuer.startsWith("https://login.microsoftonline.com/")
                    && tokenIssuer.endsWith("/v2.0");
        }
        return super.issuerMatches(tokenIssuer);
    }
}

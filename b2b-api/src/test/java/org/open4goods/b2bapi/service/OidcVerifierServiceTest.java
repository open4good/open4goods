package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.model.OidcProvider;

/**
 * Unit tests for provider verifier dispatch.
 */
class OidcVerifierServiceTest {

    @Test
    void routesCredentialToMatchingProviderVerifier() {
        final OidcUserProfile expected = new OidcUserProfile(
                OidcProvider.GOOGLE, "sub", "user@example.com", true, "User", null);
        final OidcVerifierService service = new OidcVerifierService(List.of(new StubVerifier(expected)));

        assertThat(service.verify(OidcProvider.GOOGLE, "credential")).isEqualTo(expected);
    }

    private record StubVerifier(OidcUserProfile profile) implements OidcTokenVerifier {

        @Override
        public OidcProvider provider() {
            return profile.provider();
        }

        @Override
        public OidcUserProfile verify(final String credential) {
            return profile;
        }
    }
}

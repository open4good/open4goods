package org.open4goods.sbadmin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class SbaRegistrationAuthenticationProviderTest {

    @Test
    void authenticatesTheConfiguredRegistrationIdentityWithTheRequiredRole() {
        SbaRegistrationCredentials credentials = credentials("beta-sba-client", "secret-value");

        var authentication = new SbaRegistrationAuthenticationProvider(credentials)
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("beta-sba-client", "secret-value"));

        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_XWIKIADMINGROUP");
    }

    @Test
    void rejectsAnInvalidPasswordWithoutDelegatingTheRegistrationIdentityToXwiki() {
        SbaRegistrationCredentials credentials = credentials("beta-sba-client", "secret-value");

        assertThatThrownBy(() -> new SbaRegistrationAuthenticationProvider(credentials)
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("beta-sba-client", "incorrect")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void remainsDisabledWhenTheEnvironmentDoesNotProvideBothValues() {
        SbaRegistrationCredentials credentials = credentials("beta-sba-client", "");

        assertThat(new SbaRegistrationAuthenticationProvider(credentials)
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("beta-sba-client", "anything")))
                .isNull();
    }

    private static SbaRegistrationCredentials credentials(String username, String password) {
        SbaRegistrationCredentials credentials = new SbaRegistrationCredentials();
        credentials.setUsername(username);
        credentials.setPassword(password);
        return credentials;
    }
}

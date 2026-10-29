package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ActuatorMonitorCredentials;
import org.open4goods.model.RolesConstants;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Tests the isolated Actuator monitor authentication boundary. */
class ActuatorMonitorAuthenticationProviderTest {

    @Test
    void authenticatesTheConfiguredMonitorWithTheActuatorRole() {
        var authentication = provider("beta-monitor", "secret-value")
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("beta-monitor", "secret-value"));

        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_" + RolesConstants.ACTUATOR_ADMIN_ROLE);
    }

    @Test
    void rejectsAnInvalidMonitorPassword() {
        assertThatThrownBy(() -> provider("beta-monitor", "secret-value")
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("beta-monitor", "incorrect")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void leavesOtherIdentitiesUnauthenticated() {
        assertThat(provider("beta-monitor", "secret-value")
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("consumer-user", "anything")))
                .isNull();
    }

    private static ActuatorMonitorAuthenticationProvider provider(String username, String password) {
        ActuatorMonitorCredentials credentials = new ActuatorMonitorCredentials();
        credentials.setUsername(username);
        credentials.setPassword(password);
        return new ActuatorMonitorAuthenticationProvider(credentials);
    }
}

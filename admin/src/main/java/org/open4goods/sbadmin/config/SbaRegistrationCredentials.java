package org.open4goods.sbadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds the optional local credential used only by Spring Boot Admin clients.
 *
 * <p>The credential is intentionally disabled until both values are supplied by
 * environment-specific configuration. It lets beta register monitored applications without
 * relying on the legacy XWiki authentication provider.
 */
@ConfigurationProperties(prefix = "o4g.sba.registration")
public class SbaRegistrationCredentials {

    private String username = "";

    private String password = "";

    /** Returns whether a complete local registration credential is configured. */
    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }

    /** Returns the configured registration username. */
    public String getUsername() {
        return username;
    }

    /** Sets the environment-specific registration username. */
    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    /** Returns the configured registration password. */
    public String getPassword() {
        return password;
    }

    /** Sets the environment-specific registration password. */
    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }
}

package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds the optional local identity used by Spring Boot Admin to read front-api Actuator endpoints.
 *
 * <p>The identity is independent from consumer and XWiki authentication. It remains disabled until
 * both values are supplied by environment-specific configuration.
 */
@ConfigurationProperties(prefix = "o4g.actuator.monitor")
public class ActuatorMonitorCredentials {

    private String username = "";

    private String password = "";

    /** Returns whether a complete monitor identity is configured. */
    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }

    /** Returns the monitor username. */
    public String getUsername() {
        return username;
    }

    /** Sets the environment-specific monitor username. */
    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    /** Returns the monitor password. */
    public String getPassword() {
        return password;
    }

    /** Sets the environment-specific monitor password. */
    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }
}

package org.open4goods.sbadmin.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the SBA event webhook notifier, which signs and forwards instance status
 * changes to the event-gateway for Ops. Disabled by default: both {@link #url} and
 * {@link #secret} must be supplied by environment-specific configuration (never committed to the
 * repository).
 */
@ConfigurationProperties(prefix = "o4g.sba.event-webhook")
public class SbaEventWebhookProperties {

    private String url = "";

    private String secret = "";

    private String environment = "";

    private Duration downConfirmationDelay = Duration.ofSeconds(60);

    /** Returns whether both URL and secret are configured, enabling the notifier. */
    public boolean isConfigured() {
        return !url.isBlank() && !secret.isBlank();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret == null ? "" : secret;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment == null ? "" : environment;
    }

    public Duration getDownConfirmationDelay() {
        return downConfirmationDelay;
    }

    public void setDownConfirmationDelay(Duration downConfirmationDelay) {
        this.downConfirmationDelay = downConfirmationDelay == null ? Duration.ofSeconds(60) : downConfirmationDelay;
    }
}

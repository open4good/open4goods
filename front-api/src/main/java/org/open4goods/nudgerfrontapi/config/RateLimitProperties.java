package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for request rate limiting.
 */
@Component
@ConfigurationProperties(prefix = "front.rate-limit")
public class RateLimitProperties {

    /** Maximum requests per minute for anonymous users. */
    private int anonymous = 100;

    /** Maximum requests per minute for authenticated users. */
    private int authenticated = 1000;

    public int getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(int anonymous) {
        this.anonymous = anonymous;
    }

    public int getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(int authenticated) {
        this.authenticated = authenticated;
    }
}

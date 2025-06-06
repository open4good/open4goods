package org.open4goods.services.captcha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for hCaptcha validation.
 * <p>
 * Contains the secret key for captcha verification and the Spring Security role to assign upon successful verification.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "captcha")
public record HcaptchaProperties(String key, String secretKey, String validRole) {

    public HcaptchaProperties {
        validRole = (validRole == null || validRole.isBlank()) ? "ROLE_HUMAN" : validRole;
    }

    /**
     * Returns a string representation of the captcha properties.
     * For security reasons, the secretKey is obfuscated.
     *
     * @return a string representation of the captcha properties
     */
    @Override
    public String toString() {
        return "CaptchaProperties{secretKey=****, validRole='" + validRole + '\'' + '}';
    }
}

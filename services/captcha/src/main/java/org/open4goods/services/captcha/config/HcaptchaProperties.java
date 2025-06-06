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
public class HcaptchaProperties {


    /**
     * The (public) key identifying captcha app.
     */
    private String key;



    /**
     * The secret key for captcha verification.
     */
    private String secretKey;

    /**
     * The Spring Security role assigned to users upon successful captcha verification.
     */
    private String validRole = "ROLE_HUMAN";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getValidRole() {
        return validRole;
    }

    public void setValidRole(String validRole) {
        this.validRole = validRole;
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

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}


}

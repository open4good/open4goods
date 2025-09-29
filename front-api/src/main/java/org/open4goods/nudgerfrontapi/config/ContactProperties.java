package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuration properties backing the public contact form.
 */
@Component
@ConfigurationProperties(prefix = "front.contact")
public class ContactProperties {

    /** Email address receiving contact form submissions. */
    @Schema(description = "Recipient email address for contact form submissions.", example = "contact@nudger.fr")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

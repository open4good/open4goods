package org.open4goods.nudgerfrontapi.config.properties;

import org.open4goods.model.Localisable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Blog service integration.
 * Provides base URLs for RSS feed generation across different languages.
 */
@ConfigurationProperties(prefix = "front.blog")
public class BlogProperties {

    /**
     * Base URLs for the blog RSS feed, indexed by language code.
     * Used when generating RSS feed links.
     * Example: {"default": "https://nudger.fr", "fr": "https://nudger.fr", "en":
     * "https://nudger.fr/en"}
     */
    private Localisable<String, String> baseUrls = new Localisable<>();

    public Localisable<String, String> getBaseUrls() {
        return baseUrls;
    }

    public void setBaseUrls(Localisable<String, String> baseUrls) {
        this.baseUrls = baseUrls;
    }
}

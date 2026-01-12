package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for search-related front API settings.
 */
@ConfigurationProperties(prefix = "front.search")
public class SearchProperties {

    /**
     * Suggest endpoint configuration.
     */
    private Suggest suggest = new Suggest();

    /**
     * Returns the suggest configuration.
     *
     * @return suggest configuration settings
     */
    public Suggest getSuggest() {
        return suggest;
    }

    /**
     * Updates the suggest configuration.
     *
     * @param suggest new suggest configuration settings
     */
    public void setSuggest(Suggest suggest) {
        this.suggest = suggest;
    }

    /**
     * Suggest-specific configuration properties.
     */
    public static class Suggest {

        /**
         * Enable semantic fallback for product suggestions when prefix search returns no products.
         */
        private boolean semanticFallbackEnabled = true;

        /**
         * Returns whether semantic fallback is enabled for product suggests.
         *
         * @return {@code true} when semantic fallback is enabled
         */
        public boolean isSemanticFallbackEnabled() {
            return semanticFallbackEnabled;
        }

        /**
         * Updates the semantic fallback toggle for product suggests.
         *
         * @param semanticFallbackEnabled enabled flag for semantic fallback
         */
        public void setSemanticFallbackEnabled(boolean semanticFallbackEnabled) {
            this.semanticFallbackEnabled = semanticFallbackEnabled;
        }
    }
}

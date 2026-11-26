package org.open4goods.ui.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redirection of the legacy open data download URLs of ui ({@code open-data-config.redirect-base-url}).
 * <p>
 * ui no longer depends on the {@code opendata} service and never serves a dataset file (GOU-2290): the files are
 * published on R2 by the api and downloaded from {@code data.nudger.fr}. The prefix is the one of the former
 * {@code OpenDataConfig}, so that the property rendered in ui's config tree by
 * {@code scripts/deploy/render-service-secrets.sh} (GOU-2247) keeps its name.
 * </p>
 */
@ConfigurationProperties(prefix = "open-data-config")
public class OpenDataRedirectProperties {

    /**
     * Base URL (for example {@code https://data.nudger.fr}) the legacy {@code /opendata/{name}} downloads answer a
     * permanent redirect (HTTP 301) to, as {@code {redirect-base-url}/{name}}. Empty: those URLs answer 404.
     */
    private String redirectBaseUrl = "";

    public String getRedirectBaseUrl() {
        return redirectBaseUrl;
    }

    public void setRedirectBaseUrl(String redirectBaseUrl) {
        this.redirectBaseUrl = redirectBaseUrl;
    }

    /**
     * @param name public object name of a dataset file, for example {@code gtin-open-data.zip}
     * @return {@code {redirect-base-url}/{name}}, or {@code null} when no base URL is configured
     */
    public String redirectUrl(String name) {
        if (redirectBaseUrl == null || redirectBaseUrl.isBlank()) {
            return null;
        }
        String base = redirectBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + name;
    }
}

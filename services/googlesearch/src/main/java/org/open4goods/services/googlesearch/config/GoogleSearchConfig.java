package org.open4goods.services.googlesearch.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Google Custom Search.
 * <p>
 * These properties are loaded from the application.yml (or application-test.yml for tests).
 * Example configuration:
 * <pre>
 * googlesearch:
 *   apiKey: "YOUR_API_KEY"
 *   cx: "YOUR_CX"
 *   searchUrl: "https://www.googleapis.com/customsearch/v1"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "googlesearch")
public class GoogleSearchConfig {

    private String apiKey;
    private String cx;
    private String searchUrl = "https://www.googleapis.com/customsearch/v1";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCx() {
        return cx;
    }

    public void setCx(String cx) {
        this.cx = cx;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    @Override
    public String toString() {
        return "GoogleSearchConfig{" +
                "apiKey='****'" + // Do not expose the API key in logs
                ", cx='" + cx + '\'' +
                ", searchUrl='" + searchUrl + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, cx, searchUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchConfig)) return false;
        GoogleSearchConfig that = (GoogleSearchConfig) o;
        return Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(cx, that.cx) &&
               Objects.equals(searchUrl, that.searchUrl);
    }
}

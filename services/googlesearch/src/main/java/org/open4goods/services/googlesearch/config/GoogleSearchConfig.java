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
 *   defaults:
 *     lr: "lang_en"
 *     cr: "countryUS"
 *     safe: "off"
 *     sort: ""
 *     gl: "us"
 *     hl: "en"
 *   record:
 *     enabled: true
 *     folder: "/path/to/record/folder"
 *     fromClasspath: true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "googlesearch")
public class GoogleSearchConfig {

    private String apiKey;
    private String cx;
    private String searchUrl = "https://www.googleapis.com/customsearch/v1";
    
    // New properties for recording search results.
    private boolean recordEnabled = false;
    private String recordFolder;

    // New default properties for additional search options.
    private Defaults defaults = new Defaults();

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
    
    public boolean isRecordEnabled() {
        return recordEnabled;
    }
    
    public void setRecordEnabled(boolean recordEnabled) {
        this.recordEnabled = recordEnabled;
    }
    
    public String getRecordFolder() {
        return recordFolder;
    }
    
    public void setRecordFolder(String recordFolder) {
        this.recordFolder = recordFolder;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    @Override
    public String toString() {
        return "GoogleSearchConfig{" +
                "apiKey='****'" + // Do not expose the API key in logs
                ", cx='" + cx + '\'' +
                ", searchUrl='" + searchUrl + '\'' +
                ", recordEnabled=" + recordEnabled +
                ", recordFolder='" + recordFolder + '\'' +
                ", defaults=" + defaults +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, cx, searchUrl, recordEnabled, recordFolder, defaults);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchConfig)) return false;
        GoogleSearchConfig that = (GoogleSearchConfig) o;
        return recordEnabled == that.recordEnabled &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(cx, that.cx) &&
               Objects.equals(searchUrl, that.searchUrl) &&
               Objects.equals(recordFolder, that.recordFolder) &&
               Objects.equals(defaults, that.defaults);
    }

    /**
     * Nested class to encapsulate default values for additional search options.
     */
    public static record Defaults(
            String lr,
            String cr,
            String safe,
            String sort,
            String gl,
            String hl) {

        public Defaults() {
            this("lang_fr", "countryFR", "off", "", "fr", "fr");
        }
    }
}

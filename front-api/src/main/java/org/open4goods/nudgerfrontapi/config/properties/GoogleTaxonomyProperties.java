package org.open4goods.nudgerfrontapi.config.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Google Taxonomy service.
 * Allows configuration of taxonomy URLs for different languages.
 */
@ConfigurationProperties(prefix = "front.google-taxonomy")
public class GoogleTaxonomyProperties {

    /**
     * URL for French taxonomy data from Google.
     */
    private String frenchTaxonomyUrl = "https://www.google.com/basepages/producttype/taxonomy-with-ids.fr-FR.txt";

    /**
     * URL for English taxonomy data from Google.
     */
    private String englishTaxonomyUrl = "https://www.google.com/basepages/producttype/taxonomy-with-ids.en-US.txt";

    /**
     * Additional taxonomy URLs mapped by language code.
     * Allows extending the taxonomy support to other languages.
     */
    private Map<String, String> taxonomyUrls = new HashMap<>();

    public String getFrenchTaxonomyUrl() {
        return frenchTaxonomyUrl;
    }

    public void setFrenchTaxonomyUrl(String frenchTaxonomyUrl) {
        this.frenchTaxonomyUrl = frenchTaxonomyUrl;
    }

    public String getEnglishTaxonomyUrl() {
        return englishTaxonomyUrl;
    }

    public void setEnglishTaxonomyUrl(String englishTaxonomyUrl) {
        this.englishTaxonomyUrl = englishTaxonomyUrl;
    }

    public Map<String, String> getTaxonomyUrls() {
        return taxonomyUrls;
    }

    public void setTaxonomyUrls(Map<String, String> taxonomyUrls) {
        this.taxonomyUrls = taxonomyUrls;
    }
}

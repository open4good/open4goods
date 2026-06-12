package org.open4goods.brand.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single sourcing reference backing a fact stored in the company referential.
 * Every enriched datum (manufacturing site, score, x-meta) must carry at least
 * one of these so the information remains auditable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourcedReference {

    private String url;
    private String label;
    private String retrievedAt;

    public SourcedReference() {
    }

    public SourcedReference(String url, String label, String retrievedAt) {
        this.url = url;
        this.label = label;
        this.retrievedAt = retrievedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(String retrievedAt) {
        this.retrievedAt = retrievedAt;
    }
}

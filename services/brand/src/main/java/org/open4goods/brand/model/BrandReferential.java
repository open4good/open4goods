package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Versioned brand referential containing reviewed brand mappings and review
 * suggestions.
 */
public class BrandReferential {

    private int version = 3;
    private String updatedAt;
    private String companyNameSource;
    private List<BrandReferentialEntry> brands = new ArrayList<>();
    private List<BrandSuggestion> suggestions = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCompanyNameSource() {
        return companyNameSource;
    }

    public void setCompanyNameSource(String companyNameSource) {
        this.companyNameSource = companyNameSource;
    }

    public List<BrandReferentialEntry> getBrands() {
        return brands;
    }

    public void setBrands(List<BrandReferentialEntry> brands) {
        this.brands = brands == null ? new ArrayList<>() : brands;
    }

    public List<BrandSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<BrandSuggestion> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }
}

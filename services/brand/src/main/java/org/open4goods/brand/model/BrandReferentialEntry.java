package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reviewed canonical brand definition and its known aliases.
 */
public class BrandReferentialEntry {

    private String canonicalName;
    private String normalizedName;
    private String companyName;

    @JsonProperty("company-id")
    private String companyId;

    @JsonProperty("official-domains")
    private List<String> officialDomains = new ArrayList<>();

    private String status = "reviewed";
    private List<String> synonyms = new ArrayList<>();
    private List<BrandSourceEvidence> sources = new ArrayList<>();

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public List<String> getOfficialDomains() {
        return officialDomains;
    }

    public void setOfficialDomains(List<String> officialDomains) {
        this.officialDomains = officialDomains != null ? officialDomains : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms == null ? new ArrayList<>() : synonyms;
    }

    public List<BrandSourceEvidence> getSources() {
        return sources;
    }

    public void setSources(List<BrandSourceEvidence> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }
}

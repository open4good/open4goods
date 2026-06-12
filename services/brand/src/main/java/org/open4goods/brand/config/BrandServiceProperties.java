package org.open4goods.brand.config;

/**
 * Configuration for {@link org.open4goods.brand.service.BrandService}: where the
 * brand/company referential is fetched from and how often the local cache is
 * refreshed. Defaults point at the public {@code open4good/brands-company-mapping}
 * repository. The referential is loaded through {@code RemoteFileCachingService}
 * so a transient GitHub outage does not break application startup.
 */
public class BrandServiceProperties {

    private String referentialUrl =
            "https://raw.githubusercontent.com/open4good/brands-company-mapping/refs/heads/main/brands-company-mapping.json";

    private String companyUrlTemplate =
            "https://raw.githubusercontent.com/open4good/brands-company-mapping/refs/heads/main/company/{id}.json";

    private int refreshInDays = 1;

    /**
     * @param companyId the company identifier
     * @return the resolved per-company JSON URL
     */
    public String companyUrl(String companyId) {
        return companyUrlTemplate.replace("{id}", companyId);
    }

    public String getReferentialUrl() {
        return referentialUrl;
    }

    public void setReferentialUrl(String referentialUrl) {
        this.referentialUrl = referentialUrl;
    }

    public String getCompanyUrlTemplate() {
        return companyUrlTemplate;
    }

    public void setCompanyUrlTemplate(String companyUrlTemplate) {
        this.companyUrlTemplate = companyUrlTemplate;
    }

    public int getRefreshInDays() {
        return refreshInDays;
    }

    public void setRefreshInDays(int refreshInDays) {
        this.refreshInDays = refreshInDays;
    }
}

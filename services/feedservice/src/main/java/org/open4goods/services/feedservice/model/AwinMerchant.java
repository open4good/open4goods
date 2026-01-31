package org.open4goods.services.feedservice.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AwinMerchant {

    private String description;
    private String displayUrl;
    private String logoUrl;
    private String clickThroughUrl;
    private int id;
    private String name;
    private String currencyCode;
    private PrimaryRegion primaryRegion;
    private String status;
    private String primarySector;
    private List<ValidDomain> validDomains;
    private String linkStatus;

    // Getters and setters

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }

    public void setDisplayUrl(String displayUrl) {
        this.displayUrl = displayUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public void setClickThroughUrl(String clickThroughUrl) {
        this.clickThroughUrl = clickThroughUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public PrimaryRegion getPrimaryRegion() {
        return primaryRegion;
    }

    public String getLinkStatus() {
		return linkStatus;
	}

	public void setLinkStatus(String linkStatus) {
		this.linkStatus = linkStatus;
	}

	public void setPrimaryRegion(PrimaryRegion primaryRegion) {
        this.primaryRegion = primaryRegion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimarySector() {
        return primarySector;
    }

    public void setPrimarySector(String primarySector) {
        this.primarySector = primarySector;
    }

    public List<ValidDomain> getValidDomains() {
        return validDomains;
    }

    public void setValidDomains(List<ValidDomain> validDomains) {
        this.validDomains = validDomains;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrimaryRegion {
        private String name;
        private String countryCode;

        // Getters and setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidDomain {
        private String domain;

        // Getter and setter

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }
    }
}

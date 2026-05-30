package org.open4goods.model.affiliation;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a normalized affiliation program or advertiser.
 * 
 * @author open4goods
 */
public class AffiliationProgram
{
    private String providerName;
    private String programId;
    private String advertiserName;
    private String status;
    private Set<String> countryCodes;
    private String currency;
    private Set<String> categories;
    private Integer cookieDurationDays;
    private BigDecimal clickCommission;
    private BigDecimal saleCommissionPercent;
    private BigDecimal leadCommission;
    private BigDecimal displayCpm;
    private BigDecimal epc;
    private BigDecimal conversionRate;
    private String logoUrl;
    private String portalUrl;
    private String trackingUrl;
    private String signupUrl;
    private String description;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof AffiliationProgram))
        {
            return false;
        }
        AffiliationProgram that = (AffiliationProgram) o;
        return Objects.equals(providerName, that.providerName)
                && Objects.equals(programId, that.programId)
                && Objects.equals(advertiserName, that.advertiserName)
                && Objects.equals(status, that.status)
                && Objects.equals(countryCodes, that.countryCodes)
                && Objects.equals(currency, that.currency)
                && Objects.equals(categories, that.categories)
                && Objects.equals(cookieDurationDays, that.cookieDurationDays)
                && Objects.equals(clickCommission, that.clickCommission)
                && Objects.equals(saleCommissionPercent, that.saleCommissionPercent)
                && Objects.equals(leadCommission, that.leadCommission)
                && Objects.equals(displayCpm, that.displayCpm)
                && Objects.equals(epc, that.epc)
                && Objects.equals(conversionRate, that.conversionRate)
                && Objects.equals(logoUrl, that.logoUrl)
                && Objects.equals(portalUrl, that.portalUrl)
                && Objects.equals(trackingUrl, that.trackingUrl)
                && Objects.equals(signupUrl, that.signupUrl)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(providerName, programId, advertiserName, status, countryCodes, currency,
                categories, cookieDurationDays, clickCommission, saleCommissionPercent, leadCommission,
                displayCpm, epc, conversionRate, logoUrl, portalUrl, trackingUrl, signupUrl, description);
    }

    public String getProviderName()
    {
        return providerName;
    }

    public void setProviderName(String providerName)
    {
        this.providerName = providerName;
    }

    public String getProgramId()
    {
        return programId;
    }

    public void setProgramId(String programId)
    {
        this.programId = programId;
    }

    public String getAdvertiserName()
    {
        return advertiserName;
    }

    public void setAdvertiserName(String advertiserName)
    {
        this.advertiserName = advertiserName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Set<String> getCountryCodes()
    {
        return countryCodes;
    }

    public void setCountryCodes(Set<String> countryCodes)
    {
        this.countryCodes = countryCodes;
    }

    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public Set<String> getCategories()
    {
        return categories;
    }

    public void setCategories(Set<String> categories)
    {
        this.categories = categories;
    }

    public Integer getCookieDurationDays()
    {
        return cookieDurationDays;
    }

    public void setCookieDurationDays(Integer cookieDurationDays)
    {
        this.cookieDurationDays = cookieDurationDays;
    }

    public BigDecimal getClickCommission()
    {
        return clickCommission;
    }

    public void setClickCommission(BigDecimal clickCommission)
    {
        this.clickCommission = clickCommission;
    }

    public BigDecimal getSaleCommissionPercent()
    {
        return saleCommissionPercent;
    }

    public void setSaleCommissionPercent(BigDecimal saleCommissionPercent)
    {
        this.saleCommissionPercent = saleCommissionPercent;
    }

    public BigDecimal getLeadCommission()
    {
        return leadCommission;
    }

    public void setLeadCommission(BigDecimal leadCommission)
    {
        this.leadCommission = leadCommission;
    }

    public BigDecimal getDisplayCpm()
    {
        return displayCpm;
    }

    public void setDisplayCpm(BigDecimal displayCpm)
    {
        this.displayCpm = displayCpm;
    }

    public BigDecimal getEpc()
    {
        return epc;
    }

    public void setEpc(BigDecimal epc)
    {
        this.epc = epc;
    }

    public BigDecimal getConversionRate()
    {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate)
    {
        this.conversionRate = conversionRate;
    }

    public String getLogoUrl()
    {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl)
    {
        this.logoUrl = logoUrl;
    }

    public String getPortalUrl()
    {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl)
    {
        this.portalUrl = portalUrl;
    }

    public String getTrackingUrl()
    {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl)
    {
        this.trackingUrl = trackingUrl;
    }

    public String getSignupUrl()
    {
        return signupUrl;
    }

    public void setSignupUrl(String signupUrl)
    {
        this.signupUrl = signupUrl;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}

package org.open4goods.model.affiliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a normalized affiliation promotion or voucher offer.
 * 
 * @author open4goods
 */
public class AffiliationPromotion
{
    private String providerName;
    private String programId;
    private String advertiserName;
    private String title;
    private String description;
    private String voucherCode;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String landingUrl;
    private String trackingUrl;
    private String conditions;
    private Set<String> countryCodes;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof AffiliationPromotion))
        {
            return false;
        }
        AffiliationPromotion that = (AffiliationPromotion) o;
        return Objects.equals(providerName, that.providerName)
                && Objects.equals(programId, that.programId)
                && Objects.equals(advertiserName, that.advertiserName)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(voucherCode, that.voucherCode)
                && Objects.equals(discountType, that.discountType)
                && Objects.equals(discountValue, that.discountValue)
                && Objects.equals(startDate, that.startDate)
                && Objects.equals(endDate, that.endDate)
                && Objects.equals(landingUrl, that.landingUrl)
                && Objects.equals(trackingUrl, that.trackingUrl)
                && Objects.equals(conditions, that.conditions)
                && Objects.equals(countryCodes, that.countryCodes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(providerName, programId, advertiserName, title, description, voucherCode,
                discountType, discountValue, startDate, endDate, landingUrl, trackingUrl, conditions,
                countryCodes);
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

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getVoucherCode()
    {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode)
    {
        this.voucherCode = voucherCode;
    }

    public String getDiscountType()
    {
        return discountType;
    }

    public void setDiscountType(String discountType)
    {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue()
    {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue)
    {
        this.discountValue = discountValue;
    }

    public LocalDate getStartDate()
    {
        return startDate;
    }

    public void setStartDate(LocalDate startDate)
    {
        this.startDate = startDate;
    }

    public LocalDate getEndDate()
    {
        return endDate;
    }

    public void setEndDate(LocalDate endDate)
    {
        this.endDate = endDate;
    }

    public String getLandingUrl()
    {
        return landingUrl;
    }

    public void setLandingUrl(String landingUrl)
    {
        this.landingUrl = landingUrl;
    }

    public String getTrackingUrl()
    {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl)
    {
        this.trackingUrl = trackingUrl;
    }

    public String getConditions()
    {
        return conditions;
    }

    public void setConditions(String conditions)
    {
        this.conditions = conditions;
    }

    public Set<String> getCountryCodes()
    {
        return countryCodes;
    }

    public void setCountryCodes(Set<String> countryCodes)
    {
        this.countryCodes = countryCodes;
    }
}

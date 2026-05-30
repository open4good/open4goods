package org.open4goods.model.affiliation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a normalized affiliation transaction.
 * 
 * @author open4goods
 */
public class AffiliationTransaction
{
    private String providerName;
    private String transactionId;
    private String programId;
    private Instant clickDate;
    private Instant transactionDate;
    private String status;
    private BigDecimal saleAmount;
    private BigDecimal commissionAmount;
    private String currency;
    private String subId;
    private String productId;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof AffiliationTransaction))
        {
            return false;
        }
        AffiliationTransaction that = (AffiliationTransaction) o;
        return Objects.equals(providerName, that.providerName)
                && Objects.equals(transactionId, that.transactionId)
                && Objects.equals(programId, that.programId)
                && Objects.equals(clickDate, that.clickDate)
                && Objects.equals(transactionDate, that.transactionDate)
                && Objects.equals(status, that.status)
                && Objects.equals(saleAmount, that.saleAmount)
                && Objects.equals(commissionAmount, that.commissionAmount)
                && Objects.equals(currency, that.currency)
                && Objects.equals(subId, that.subId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(providerName, transactionId, programId, clickDate, transactionDate, status,
                saleAmount, commissionAmount, currency, subId, productId);
    }

    public String getProviderName()
    {
        return providerName;
    }

    public void setProviderName(String providerName)
    {
        this.providerName = providerName;
    }

    public String getTransactionId()
    {
        return transactionId;
    }

    public void setTransactionId(String transactionId)
    {
        this.transactionId = transactionId;
    }

    public String getProgramId()
    {
        return programId;
    }

    public void setProgramId(String programId)
    {
        this.programId = programId;
    }

    public Instant getClickDate()
    {
        return clickDate;
    }

    public void setClickDate(Instant clickDate)
    {
        this.clickDate = clickDate;
    }

    public Instant getTransactionDate()
    {
        return transactionDate;
    }

    public void setTransactionDate(Instant transactionDate)
    {
        this.transactionDate = transactionDate;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public BigDecimal getSaleAmount()
    {
        return saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount)
    {
        this.saleAmount = saleAmount;
    }

    public BigDecimal getCommissionAmount()
    {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount)
    {
        this.commissionAmount = commissionAmount;
    }

    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public String getSubId()
    {
        return subId;
    }

    public void setSubId(String subId)
    {
        this.subId = subId;
    }

    public String getProductId()
    {
        return productId;
    }

    public void setProductId(String productId)
    {
        this.productId = productId;
    }
}

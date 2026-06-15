package org.open4goods.b2bapi.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

/**
 * Product Data API facet and billing catalog loaded from {@code b2b-catalog.yml}.
 */
@Validated
@PropertySource(value = "classpath:b2b-catalog.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "b2b")
public class BillingCatalogProperties {

    @Valid
    private Map<String, Facet> facets = new LinkedHashMap<>();

    @Valid
    private Billing billing = new Billing();

    public Map<String, Facet> getFacets() {
        return facets;
    }

    public void setFacets(final Map<String, Facet> facets) {
        this.facets = facets;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(final Billing billing) {
        this.billing = billing;
    }

    public static class Facet {

        @NotBlank
        private String path;

        @Positive
        private int credits;

        @NotBlank
        private String doc;

        @NotBlank
        private String billableWhen;

        public String getPath() {
            return path;
        }

        public void setPath(final String path) {
            this.path = path;
        }

        public int getCredits() {
            return credits;
        }

        public void setCredits(final int credits) {
            this.credits = credits;
        }

        public String getDoc() {
            return doc;
        }

        public void setDoc(final String doc) {
            this.doc = doc;
        }

        public String getBillableWhen() {
            return billableWhen;
        }

        public void setBillableWhen(final String billableWhen) {
            this.billableWhen = billableWhen;
        }
    }

    public static class Billing {

        @Valid
        private Map<String, Pack> packs = new LinkedHashMap<>();

        @Valid
        private Map<String, Subscription> subscriptions = new LinkedHashMap<>();

        public Map<String, Pack> getPacks() {
            return packs;
        }

        public void setPacks(final Map<String, Pack> packs) {
            this.packs = packs;
        }

        public Map<String, Subscription> getSubscriptions() {
            return subscriptions;
        }

        public void setSubscriptions(final Map<String, Subscription> subscriptions) {
            this.subscriptions = subscriptions;
        }
    }

    public static class Pack {

        @Positive
        private BigDecimal amountEur;

        @Positive
        private int credits;

        private String stripePriceId = "";

        public BigDecimal getAmountEur() {
            return amountEur;
        }

        public void setAmountEur(final BigDecimal amountEur) {
            this.amountEur = amountEur;
        }

        public int getCredits() {
            return credits;
        }

        public void setCredits(final int credits) {
            this.credits = credits;
        }

        public String getStripePriceId() {
            return stripePriceId;
        }

        public void setStripePriceId(final String stripePriceId) {
            this.stripePriceId = stripePriceId;
        }
    }

    public static class Subscription {

        @Positive
        private BigDecimal amountEur;

        @Positive
        private int monthlyCredits;

        @Min(0)
        private int rolloverCapMonths;

        private String stripePriceId = "";

        public BigDecimal getAmountEur() {
            return amountEur;
        }

        public void setAmountEur(final BigDecimal amountEur) {
            this.amountEur = amountEur;
        }

        public int getMonthlyCredits() {
            return monthlyCredits;
        }

        public void setMonthlyCredits(final int monthlyCredits) {
            this.monthlyCredits = monthlyCredits;
        }

        public int getRolloverCapMonths() {
            return rolloverCapMonths;
        }

        public void setRolloverCapMonths(final int rolloverCapMonths) {
            this.rolloverCapMonths = rolloverCapMonths;
        }

        public String getStripePriceId() {
            return stripePriceId;
        }

        public void setStripePriceId(final String stripePriceId) {
            this.stripePriceId = stripePriceId;
        }
    }
}

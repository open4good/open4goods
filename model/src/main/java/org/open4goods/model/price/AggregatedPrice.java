package org.open4goods.model.price;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Set;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.open4goods.model.Standardisable;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.Validable;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.ProductCondition;
import org.springframework.data.annotation.Transient;

/**
 * Immutable representation of an aggregated price offer.
 */
public record AggregatedPrice(
        String datasourceName,
        String offerName,
        String url,
        Double compensation,
        ProductCondition productState,
        @Transient String affiliationToken,
        Double price,
        Currency currency,
        Long timeStamp) implements Validable, Standardisable, Comparable<Double> {

    /** Shared price formatter. */
    public static final DecimalFormat numberFormater = new DecimalFormat("0.#");

    /** Build from a {@link DataFragment}. */
    public AggregatedPrice(DataFragment df) {
        this(df.getDatasourceName(),
             df.longestName(),
             df.affiliatedUrlIfPossible(),
             null,
             df.getProductState(),
             null,
             df.getPrice().getPrice(),
             df.getPrice().getCurrency(),
             df.getLastIndexationDate());
    }

    /** Constructor for average price representation. */
    public AggregatedPrice(double price, Currency currency) {
        this(null, null, null, null, null, null, price, currency, System.currentTimeMillis());
    }

    /**
     * Datasource name without top-level domain.
     */
    public String shortDataSourceName() {
        if (datasourceName == null) {
            return "ERROR_NOT_SET";
        }
        int i = datasourceName.indexOf('.');
        return i == -1 ? datasourceName : datasourceName.substring(0, i);
    }

    /**
     * Localised duration since last indexation.
     */
    public String ago(Locale locale) {
        long duration = System.currentTimeMillis() - timeStamp;
        Period period;
        if (duration < 3_600_000) {
            DurationFieldType[] min = {DurationFieldType.minutes(), DurationFieldType.seconds()};
            period = new Period(duration, PeriodType.forFields(min)).normalizedStandard();
        } else {
            DurationFieldType[] full = {DurationFieldType.days(), DurationFieldType.hours()};
            period = new Period(duration, PeriodType.forFields(full)).normalizedStandard();
        }
        PeriodFormatter formatter = PeriodFormat.wordBased(locale);
        return formatter.print(period);
    }

    /** Default locale formatted duration. */
    public String formatedDuration() {
        return ago(Locale.FRANCE); // TODO: localize dynamically
    }

    /**
     * Human readable price.
     */
    public String shortPrice() {
        boolean isInt = price == Math.rint(price);
        return isInt ? String.valueOf(price.intValue()) : numberFormater.format(price);
    }

    // ---------------------------------------------------------------------
    // Contract methods
    // ---------------------------------------------------------------------
    @Override
    public int compareTo(Double o) {
        return price.compareTo(o);
    }

    @Override
    public void validate() throws ValidationException {
        if (currency == null || price == null) {
            throw new ValidationException("Invalid price");
        }
    }

    @Override
    public Set<Standardisable> standardisableChildren() {
        return Set.of(this);
    }

    @Override
    public void standardize(StandardiserService standardiser, Currency currency) {
        // same behavior as Price
        standardiser.standarise(new Price(price, currency), currency);
    }

    // ---------------------------------------------------------------------
    // Compatibility helpers
    // ---------------------------------------------------------------------

    /** Accessor mirroring former getter. */
    public Double getPrice() { return price; }

    /** Accessor mirroring former getter. */
    public Currency getCurrency() { return currency; }

    /** Accessor mirroring former getter. */
    public Long getTimeStamp() { return timeStamp; }

    /** Accessor mirroring former getter. */
    public String getDatasourceName() { return datasourceName; }

    /** Accessor mirroring former getter. */
    public String getOfferName() { return offerName; }

    /** Accessor mirroring former getter. */
    public String getUrl() { return url; }

    /** Accessor mirroring former getter. */
    public Double getCompensation() { return compensation; }

    /** Accessor mirroring former getter. */
    public ProductCondition getProductState() { return productState; }

    /** Accessor mirroring former getter. */
    public String getAffiliationToken() { return affiliationToken; }

    // ---------------------------------------------------------------------
    // Behavioral helpers
    // ---------------------------------------------------------------------
    public boolean lowerThan(AggregatedPrice p) {
        if (p == null) return false;
        if (!currency.equals(p.currency)) {
            return price < p.price; // different currencies, best effort
        }
        return price < p.price;
    }

    public boolean greaterThan(AggregatedPrice p) {
        if (p == null) return false;
        if (!currency.equals(p.currency)) {
            return price > p.price;
        }
        return price > p.price;
    }

    // ---------------------------------------------------------------------
    // Withers
    // ---------------------------------------------------------------------
    public AggregatedPrice withCompensation(Double c) {
        return new AggregatedPrice(datasourceName, offerName, url, c, productState, affiliationToken, price, currency, timeStamp);
    }

    public AggregatedPrice withAffiliationToken(String token) {
        return new AggregatedPrice(datasourceName, offerName, url, compensation, productState, token, price, currency, timeStamp);
    }

    public AggregatedPrice withTimeStamp(Long ts) {
        return new AggregatedPrice(datasourceName, offerName, url, compensation, productState, affiliationToken, price, currency, ts);
    }
}

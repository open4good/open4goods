package org.open4goods.model.price;

/**
 * Historical price entry consisting of a timestamp and the associated price.
 */
public record PriceHistory(Long timestamp, Double price) {

    /**
     * Creates a history entry from an {@link AggregatedPrice} instance.
     *
     * @param minPrice the aggregated price to build from
     */
    public PriceHistory(AggregatedPrice minPrice) {
        this(minPrice.getTimeStamp(), minPrice.getPrice());
    }

    /**
     * Number of days corresponding to this history timestamp.
     *
     * @return the day count
     */
    public Long getDay() {
        return timestamp / (1000 * 60 * 60 * 24);
    }

    // Compatibility accessors ----------------------------------------------

    /** @return timestamp accessor mirroring the former getter */
    public Long getTimestamp() {
        return timestamp;
    }

    /** @return price accessor mirroring the former getter */
    public Double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return timestamp + ":" + price;
    }
}

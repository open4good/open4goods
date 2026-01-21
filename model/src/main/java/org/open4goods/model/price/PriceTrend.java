package org.open4goods.model.price;

import java.util.List;
import java.util.Locale;

import org.open4goods.model.util.TimeAgoFormatter;

/**
 * Represents the price trend of a product, including price variation,
 * period between price updates, and historical comparisons.
 *

 */
public record PriceTrend(
                Integer trend,
                Long period,
                Double actualPrice,
                Double lastPrice,
                Double variation,
                Double historicalLowestPrice,
                Double historicalVariation) {

	/**
	 * Computes a PriceTrend instance based on price history and current price.
	 *
	 * @param history List of past price history entries, must contain at least 2 entries.
	 * @param actual  The current aggregated price information.
	 * @return A populated PriceTrend object.
	 */
        public static PriceTrend of(List<PriceHistory> history, AggregatedPrice actual) {
                if (history.size() > 1 && actual != null) {
                        PriceHistory last = history.get(history.size() - 2);
                        double actualVal = actual.getPrice();
                        double lastVal = last.getPrice();
                        long timeDiff = actual.getTimeStamp() - last.getTimestamp();

                        double variation = actualVal - lastVal;
                        double historicalLowest = history.stream()
                                        .mapToDouble(PriceHistory::getPrice)
                                        .min()
                                        .orElse(0.0);
                        double historicalVar = actualVal - historicalLowest;

                        return new PriceTrend(
                                        Double.compare(variation, 0),
                                        timeDiff,
                                        actualVal,
                                        lastVal,
                                        variation,
                                        historicalLowest,
                                        historicalVar);
                }

                return new PriceTrend(0, null, null, null, null, null, null);
        }

	/**
	 * Formats the duration since the last price change in a localized format.
	 *
	 * @return A localized string describing the time since the last price change.
	 */
        public String formatedDuration() {
                return ago(Locale.getDefault(), period);
        }

	/**
	 * Localized "ago" time formatter.
	 *
	 * @param locale   The locale to use for formatting.
	 * @param duration The duration in milliseconds.
	 * @return A localized string like "2 days", "3 minutes", etc.
	 */
	public String ago(Locale locale, long duration) {
		return TimeAgoFormatter.formatDuration(locale, duration);
	}

	/**
	 * @return The absolute value of the price variation.
	 */
	public Double absVariation() {
		return variation != null ? Math.abs(variation) : null;
	}

	/**
	 * @return The percentage price variation compared to the last price.
	 *         Returns null if lastPrice is null or zero.
	 */
	public Double percentVariation() {
		if (lastPrice == null || lastPrice == 0.0) return null;
		return (variation / lastPrice) * 100;
	}

        // Compatibility accessors ------------------------------------------

        public Integer getTrend() {
                return trend;
        }

        public Long getPeriod() {
                return period;
        }

        public Double getActualPrice() {
                return actualPrice;
        }

        public Double getLastPrice() {
                return lastPrice;
        }

        public Double getVariation() {
                return variation;
        }

        public Double getHistoricalLowestPrice() {
                return historicalLowestPrice;
        }

        public Double getHistoricalVariation() {
                return historicalVariation;
        }
}

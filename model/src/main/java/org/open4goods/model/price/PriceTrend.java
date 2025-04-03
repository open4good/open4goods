package org.open4goods.model.price;

import java.util.List;
import java.util.Locale;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

/**
 * Represents the price trend of a product, including price variation,
 * period between price updates, and historical comparisons.
 *

 */
public class PriceTrend {

	private Integer trend; // 1 = increase, -1 = decrease, 0 = stable or unknown
	private Long period; // milliseconds between the two last price records
	private Double actualPrice;
	private Double lastPrice;
	private Double variation;
	private Double historicalLowestPrice;
	private Double historicalVariation;

	/**
	 * Computes a PriceTrend instance based on price history and current price.
	 *
	 * @param history List of past price history entries, must contain at least 2 entries.
	 * @param actual  The current aggregated price information.
	 * @return A populated PriceTrend object.
	 */
	public static PriceTrend of(List<PriceHistory> history, AggregatedPrice actual) {
		PriceTrend trend = new PriceTrend();

		if (history.size() > 1 && actual != null) {
			PriceHistory last = history.get(history.size() - 2);
			double actualVal = actual.getPrice();
			double lastVal = last.getPrice();
			long timeDiff = actual.getTimeStamp() - last.getTimestamp();

			trend.setActualPrice(actualVal);
			trend.setLastPrice(lastVal);
			trend.setVariation(actualVal - lastVal);
			trend.setPeriod(timeDiff);
			trend.setTrend(Double.compare(trend.getVariation(), 0));
			trend.setHistoricalLowestPrice(
				history.stream()
					.mapToDouble(PriceHistory::getPrice)
					.min()
					.orElse(0.0)
			);
			trend.setHistoricalVariation(actualVal - trend.getHistoricalLowestPrice());
		} else {
			trend.setTrend(0);
		}

		return trend;
	}

	/**
	 * Formats the duration since the last price change in a localized format.
	 *
	 * @return A localized string describing the time since the last price change.
	 */
	public String formatedDuration() {
		return ago(Locale.FRANCE, period); // TODO: localize dynamically
	}

	/**
	 * Localized "ago" time formatter.
	 *
	 * @param locale   The locale to use for formatting.
	 * @param duration The duration in milliseconds.
	 * @return A localized string like "2 days", "3 minutes", etc.
	 */
	public String ago(Locale locale, long duration) {
		Period period;

		if (duration < 3_600_000) { // less than 1 hour
			DurationFieldType[] min = { DurationFieldType.minutes(), DurationFieldType.seconds() };
			period = new Period(duration, PeriodType.forFields(min)).normalizedStandard();
		} else {
			DurationFieldType[] full = { DurationFieldType.days(), DurationFieldType.hours() };
			period = new Period(duration, PeriodType.forFields(full)).normalizedStandard();
		}

		PeriodFormatter formatter = PeriodFormat.wordBased(locale);
		return formatter.print(period);
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

	// Getters and setters

	public Integer getTrend() {
		return trend;
	}

	public void setTrend(Integer trend) {
		this.trend = trend;
	}

	public Long getPeriod() {
		return period;
	}

	public void setPeriod(Long period) {
		this.period = period;
	}

	public Double getActualPrice() {
		return actualPrice;
	}

	public void setActualPrice(Double actualPrice) {
		this.actualPrice = actualPrice;
	}

	public Double getLastPrice() {
		return lastPrice;
	}

	public void setLastPrice(Double lastPrice) {
		this.lastPrice = lastPrice;
	}

	public Double getVariation() {
		return variation;
	}

	public void setVariation(Double variation) {
		this.variation = variation;
	}

	public Double getHistoricalLowestPrice() {
		return historicalLowestPrice;
	}

	public void setHistoricalLowestPrice(Double historicalLowestPrice) {
		this.historicalLowestPrice = historicalLowestPrice;
	}

	public Double getHistoricalVariation() {
		return historicalVariation;
	}

	public void setHistoricalVariation(Double historicalVariation) {
		this.historicalVariation = historicalVariation;
	}
}

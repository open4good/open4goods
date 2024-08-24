package org.open4goods.commons.model.product;

import java.util.List;
import java.util.Locale;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

// TODO : Aggregation batched trend seems buggy.
// Should deport this to PriceAggregationService
public class PriceTrend {
	
	private Integer trend;
	private Long period;
	private Double actualPrice;
	private Double lastPrice;
	private Double variation;
	private Double historicalLowestPrice;
	private Double historicalVariation;
	
	
	
	public static PriceTrend of(List<PriceHistory> history) {
		PriceTrend trend = new PriceTrend();
        if(history.size() > 1) {
            PriceHistory actual = history.getLast();
            PriceHistory last = history.get(history.size() - 2);
            trend.setActualPrice(actual.getPrice());
            trend.setLastPrice(last.getPrice());
            trend.setVariation(actual.getPrice() - last.getPrice());
            trend.setPeriod(actual.getTimestamp() - last.getTimestamp());
            trend.setTrend(trend.getVariation() > 0 ? 1 : (trend.getVariation() < 0 ? -1 : 0));
            trend.setHistoricalLowestPrice(history.stream().mapToDouble(PriceHistory::getPrice).min().orElse(0));
            trend.setHistoricalVariation(trend.getActualPrice() - trend.getHistoricalLowestPrice());
        }
        
        
        return trend;
    }


	public String formatedDuration() {
		// TODO : LOCALIZE
		return ago(Locale.FRANCE, period);
	}
	
	
	/**
	 * TODO : merge with the one on Product()
	 * @return a localised formated duration of when the product was last indexed
	 */
	public String ago(Locale locale, long duration) {

		
		Period period;
		if (duration < 3600000) {
			DurationFieldType[] min = { DurationFieldType.minutes(), DurationFieldType.seconds() };
			period = new Period(duration, PeriodType.forFields(min)).normalizedStandard();
		} else {
			DurationFieldType[] full = { DurationFieldType.days(), DurationFieldType.hours() };
			period = new Period(duration, PeriodType.forFields(full)).normalizedStandard();

		}
		
		PeriodFormatter formatter = PeriodFormat.wordBased();

		String ret = (formatter. print(period));
		
		
		return ret;
	}
	
	
	public Double absVariation() {
		return Math.abs(variation);
	}
	
	
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


package org.open4goods.model.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.Standardisable;
import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedPrices implements Standardisable {

	@Field(index = false, store = false, type = FieldType.Object)
	private Set<AggregatedPrice> offers = new HashSet<>();

	@Field(index = false, store = false, type = FieldType.Object)
	private AggregatedPrice minPrice;

	@Field(index = false, store = false, type = FieldType.Object)
	private List<PriceHistory> history = new ArrayList<>();
	
	@Field(index = false, store = false, type = FieldType.Integer)
	// Price evolution trend : 
	// 0 -> equals
	// 1 -> Increasing
	// 2 -> Decreasing
	private Integer trend= 0;
	
	
	
	
//	@Field(index = false, store = false, type = FieldType.Object)
//	private AggregatedPrice maxPrice;
//
//	@Field(index = false, store = false, type = FieldType.Object)
//	private AggregatedPrice avgPrice;
	
	
	/**
	 * Sort offers, always the cheapest compensated order first, then price sorting
	 * @return
	 */
	public List<AggregatedPrice> sortedOffers() {
		// Find the cheapest affiliated
		final AggregatedPrice cheapest = offers.stream()
				.filter(d -> d.isAffiliated())
		.sorted(Comparator.comparingDouble(AggregatedPrice::getPrice)).findFirst().orElse(null);

		
		// Find the cheapest in any case
		final List<AggregatedPrice> res = offers.stream()
				.sorted(Comparator.comparingDouble(AggregatedPrice::getPrice))
				.collect(Collectors.toList());

		res.remove(cheapest);
		res.add(0, cheapest);

		return res;


	}

	@Override
	public Set<Standardisable> standardisableChildren() {
		final Set<Standardisable> ret = new HashSet<>();
		ret.addAll(offers);
		ret.add(minPrice);
//		ret.add(maxPrice);
//		ret.add(avgPrice);

		return ret;
	}

	@Override
	public void standardize(final StandardiserService standardiser, final Currency currency) {
		// NOTE(gof) : handled externally

	}

	/**
	 * Number of affiliated offers
	 * @return
	 */
	public Long affiliatedOffersCount() {
		return offers.stream().filter(e -> e.isAffiliated()).count();
	}

	/////////////////////////////////////////////
	// Getters / Setters
	////////////////////////////////////////////
	
	public Set<AggregatedPrice> getOffers() {
		return offers;
	}

	public void setOffers(Set<AggregatedPrice> offers) {
		this.offers = offers;
	}

	public AggregatedPrice getMinPrice() {
		return minPrice;
	}

	public void setMinPrice(AggregatedPrice minPrice) {
		this.minPrice = minPrice;
	}

	public List<PriceHistory> getHistory() {
		return history;
	}

	public void setHistory(List<PriceHistory> history) {
		this.history = history;
	}

	public Integer getTrend() {
		return trend;
	}

	public void setTrend(Integer trend) {
		this.trend = trend;
	}

//	public AggregatedPrice getMaxPrice() {
//		return maxPrice;
//	}
//
//	public void setMaxPrice(AggregatedPrice maxPrice) {
//		this.maxPrice = maxPrice;
//	}
//
//	public AggregatedPrice getAvgPrice() {
//		return avgPrice;
//	}
//
//	public void setAvgPrice(AggregatedPrice avgPrice) {
//		this.avgPrice = avgPrice;
//	}



}

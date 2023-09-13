
package org.open4goods.model.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.Standardisable;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.ProductState;
import org.open4goods.services.StandardiserService;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedPrices implements Standardisable {

	@Field(index = false, store = false, type = FieldType.Object)
	private Set<AggregatedPrice> offers = new HashSet<>();

	@Field(index = false, store = false, type = FieldType.Object)
	private AggregatedPrice minPrice;

	@Field(index = false, store = false, type = FieldType.Object)
	private List<PriceHistory> newPricehistory = new ArrayList<>();

	@Field(index = false, store = false, type = FieldType.Object)
	private List<PriceHistory> occasionPricehistory = new ArrayList<>();
		
	@Field(index = false, store = false, type = FieldType.Integer)
	// Price evolution trend :
	// 0 -> equals
	// 1 -> Increasing
	// 2 -> Decreasing
	private Integer trend= 0;

	@Field(index = true, store = false, type = FieldType.Keyword)

	// Contains the conditions for this product. Shortcut for elastic queryng
	private Set<ProductState> conditions = new HashSet<>();

	public List<AggregatedPrice> newOffers() {
		List<AggregatedPrice> soffers = sortedOffers(ProductState.NEW);
		if (null == soffers) {
			return new ArrayList<>();
		} else {
			return soffers.stream().filter(e-> e != null).filter(e->e.getProductState().equals(ProductState.NEW)).toList();	
			
		}
	}
	
	
	
	
	/**
	 * Sort offers, always the cheapest compensated order first, then price sorting
	 * @param productState 
	 * @return
	 */
	public List<AggregatedPrice> sortedOffers(ProductState productState) {
		// Find the cheapest affiliated
		final AggregatedPrice cheapest = offers.stream()
				.filter(e->e.getProductState().equals(productState))
				.sorted(Comparator.comparingDouble(AggregatedPrice::getPrice)).findFirst().orElse(null);

		// Find the cheapest in any case
		final List<AggregatedPrice> res = offers.stream()
				.sorted(Comparator.comparingDouble(AggregatedPrice::getPrice))
				.collect(Collectors.toList());

		res.remove(cheapest);
		res.add(0, cheapest);

		return res;


	}
	

	public List<PriceHistory> getHistory(ProductState state) {

		return switch (state) {
	    case OCCASION  -> this.occasionPricehistory;
	    case NEW -> this.newPricehistory;
		default -> throw new IllegalArgumentException("Unexpected value: " + state);

	};
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
	
	public AggregatedPrice getMinPrice(ProductState state) {
		return offers.stream().filter(e -> e.getProductState().equals(state)).min(Comparator.comparing(AggregatedPrice::getPrice)).orElseGet(null) ;
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


	public Integer getTrend() {
		return trend;
	}

	public void setTrend(Integer trend) {
		this.trend = trend;
	}

	public Set<ProductState> getConditions() {
		return conditions;
	}

	public void setConditions(Set<ProductState> conditions) {
		this.conditions = conditions;
	}




	public List<PriceHistory> getNewPricehistory() {
		return newPricehistory;
	}

	public void setNewPricehistory(List<PriceHistory> newPricehistory) {
		this.newPricehistory = newPricehistory;
	}

	public List<PriceHistory> getOccasionPricehistory() {
		return occasionPricehistory;
	}

	public void setOccasionPricehistory(List<PriceHistory> occasionPricehistory) {
		this.occasionPricehistory = occasionPricehistory;
	}


}

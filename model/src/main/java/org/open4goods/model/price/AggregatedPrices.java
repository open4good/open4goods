
package org.open4goods.model.price;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.Standardisable;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;

public class AggregatedPrices implements Standardisable {

	private Set<AggregatedPrice> offers = new HashSet<>();

	private AggregatedPrice minPrice;

	private List<PriceHistory> newPricehistory = new ArrayList<>();

	private List<PriceHistory> occasionPricehistory = new ArrayList<>();
		
	// Price evolution trend :
	// 0 -> equals
	// 1 -> Increasing
	// -1 -> Decreasing
	// TODO(perf, p2) : Remove, migration on trends
//	private Integer trend= 0;
	
	
	/** Trends by product condition **/
	private Map<ProductCondition, Integer> trends = new HashMap<>();


	// Contains the conditions for this product. Shortcut for elastic queryng
	private Set<ProductCondition> conditions = new HashSet<>();

	public List<AggregatedPrice> newOffers() {
		return sortedOffers(ProductCondition.NEW);
	}
	
	
	public List<AggregatedPrice> occasionOffers() {
		return sortedOffers(ProductCondition.OCCASION);
	}
	
	
	/**
	 * Sort offers, always the cheapest compensated order first, then price sorting
	 * @param productState 
	 * @return
	 */
	public List<AggregatedPrice> sortedOffers(ProductCondition productState) {

		// Find the cheapest in any case
		final List<AggregatedPrice> res = offers.stream()
				.sorted(Comparator.comparingDouble(AggregatedPrice::getPrice))
				.filter(e->e.getProductState().equals(productState))
				.collect(Collectors.toList());

		return res;


	}
	
	public AggregatedPrice bestOccasionOffer() {
		return bestOffer(ProductCondition.OCCASION);
	}

	public AggregatedPrice bestNewOffer() {
		return bestOffer(ProductCondition.NEW);
	}

	
	public AggregatedPrice bestOffer(ProductCondition condition) {
		var list = sortedOffers(condition);
		if (list != null && list.size() > 0 ) {
			return list.getFirst();
		}
		return null;
		
	}

	
	public List<PriceHistory> getHistory(ProductCondition state) {

		return switch (state) {
	    case OCCASION  -> this.occasionPricehistory;
	    case NEW -> this.newPricehistory;
		default -> throw new IllegalArgumentException("Unexpected value: " + state);

		};
	}
	 /**
     * Returns the lowest historical price for the given condition (NEW or OCCASION).
     *
     * @param condition the product condition
     * @return the PriceHistory object with the lowest price, or null if history is empty
     */
    public PriceHistory getHistoryLowest(ProductCondition condition) {
        List<PriceHistory> history = getHistory(condition);

        return history.stream()
                .min(Comparator.comparing(PriceHistory::getPrice))
                .orElse(null);
    }
    
    public boolean isHistoricalLowest(AggregatedPrice price) {
    	PriceHistory lowest = getHistoryLowest(price.getProductState());
    	return (lowest.getPrice().equals(bestOffer(price.getProductState()).getPrice()));
    }

    /**
     * Returns the highest historical price for the given condition (NEW or OCCASION).
     *
     * @param condition the product condition
     * @return the PriceHistory object with the highest price, or null if history is empty
     */
    public PriceHistory getHistoryHighest(ProductCondition condition) {
        List<PriceHistory> history = getHistory(condition);

        return history.stream()
                .max(Comparator.comparing(PriceHistory::getPrice))
                .orElse(null);
    }

    /**
     * Calculates the average price from the historical data for the given condition.
     *
     * @param condition the product condition
     * @return the average historical price, or null if history is empty
     */
    public Double getHistoryAverage(ProductCondition condition) {
        List<PriceHistory> history = getHistory(condition);

        return history.stream()
                .mapToDouble(PriceHistory::getPrice)
                .average()
                .orElse(Double.NaN); // Can also return null if preferred
    }
	
	
	
	
	/**
	 * Get the gap between cureent best price and the lowest price ever measured
	 * @return
	 */
	public Double historyPriceGap() {
		List<PriceHistory> histo = getHistory(ProductCondition.NEW);
		if (null == histo || histo.isEmpty()) {
			return null;
		}
		PriceHistory lowest = histo.stream().min(Comparator.comparing(PriceHistory::getPrice)).get();
		
		if (null != lowest) {
            
			if (minPrice.getPrice() > lowest.getPrice()) {
				return lowest.getPrice();		
			} else {
				return minPrice.getPrice();
			}
        }
		
		return null;
	}
	

	@Override
	public Set<Standardisable> standardisableChildren() {
        final Set<Standardisable> ret = new HashSet<>(offers);
		ret.add(minPrice);
		//		ret.add(maxPrice);
		//		ret.add(avgPrice);

		return ret;
	}

	@Override
	public void standardize(final StandardiserService standardiser, final Currency currency) {
		// NOTE(gof) : handled externally

	}
	
	public Optional<AggregatedPrice> getMinPrice(ProductCondition state) {
		return offers.stream().filter(e -> e.getProductState().equals(state)) .min(Comparator.comparing(AggregatedPrice::getPrice)) ;
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




	public Map<ProductCondition, Integer> getTrends() {
		return trends;
	}


	public void setTrends(Map<ProductCondition, Integer> trends) {
		this.trends = trends;
	}


	public Set<ProductCondition> getConditions() {
		return conditions;
	}

	public void setConditions(Set<ProductCondition> conditions) {
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

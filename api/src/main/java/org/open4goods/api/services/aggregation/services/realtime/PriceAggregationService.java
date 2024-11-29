
package org.open4goods.api.services.aggregation.services.realtime;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.model.constants.ProductCondition;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.product.AggregatedPrice;
import org.open4goods.commons.model.product.AggregatedPrices;
import org.open4goods.commons.model.product.PriceHistory;
import org.open4goods.commons.model.product.Product;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * This service compute price infos from DataFragments computations if not in
 * stock
 *
 *
 *
 * @author goulven
 *
 */
public class PriceAggregationService extends AbstractAggregationService {
	
	/**
	 * Allows to compute the incomes, it is the average percent reversed to Nudger by affiliation platforms
	 */
	private static Double averageAffiliationRatio = 0.05;
	
	/**
	 * The ratio that allows to estimate benefits from revenue
	 */
	private static Double incomesToBenefitsRatio = 0.75;
	
	/**
	 * The percent of benefits reversed
	 */
	private static Double percentBenefitsReversed = 0.1;
	

	public PriceAggregationService(final Logger logger) {
		super(logger);
	}

	@Override
	public  Map<String, Object> onDataFragment(final DataFragment fragment, final Product aggregatedData,VerticalConfig vConf) throws AggregationSkipException {

		if (!fragment.hasPrice()) {
			dedicatedLogger.warn("No price for data fragment {}, skipping", fragment );
		} else if (fragment.getPrice().getPrice() == 0.0) {
			// Checking price is not 0, can happens
			dedicatedLogger.info("Price is 0 for datafragment {}, skipping", fragment);
		} else {		
			// Adding the price in the price list, we fill filter and remove outdated in the onProduct() méthod
			AggregatedPrice aggPrice = new AggregatedPrice(fragment);
			aggregatedData.getPrice().getOffers().add(aggPrice);			
		}
		
		// Calling the stateless handling
		onProduct(aggregatedData, vConf);
		return null;
	}

	
	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		///////////////////
		// Filtering : 
		//////////////////
		
		// Key is providerName + name
		final Map<String, AggregatedPrice> reducedPrices = new HashMap<>();

		// Adding current price in the list of all prices

		for (final AggregatedPrice df : data.getPrice().getOffers()) {

			// Filtering : removing outdated prices
			if (System.currentTimeMillis() - ProductRepository.VALID_UNTIL_DURATION > df.getTimeStamp()) {
				dedicatedLogger.info("price too old for datafragment {}, removing it", df);

			} else {

				final String key = pricMerchanteKey(df);
				// Filtering : keeping lowest prices per provider and offer names	
				if (null == reducedPrices.get(key) || reducedPrices.get(key).getPrice() > df.getPrice()) {
					reducedPrices.put(key, df);
				}
			}
		}

		final Set<AggregatedPrice> prices = new HashSet<>(reducedPrices.values());

		////////////////////////////
		// Set the contribution amount 
		////////////////////////////
		for (AggregatedPrice price : prices) {
			price.setCompensation(computeEstimatedContribution(price.getPrice()));
		}

		AggregatedPrices aggPrices = data.getPrice();

		/////////////////////////
		// Prices computation
		////////////////////////

		// Compute current prices
		computeMinPrice(prices, aggPrices);

		// set Number of offers
		data.setOffersCount(prices.size());
		
		// Reseting prices (reducing)
		data.getPrice().setOffers(Sets.newHashSet(prices));

		// Computing / incrementing history
		computePriceHistory(aggPrices, ProductCondition.OCCASION);		
		computePriceHistory(aggPrices, ProductCondition.NEW);

		// Setting the product state summary
		aggPrices.getConditions().clear();
		aggPrices.getOffers().forEach(i -> aggPrices.getConditions().add(i.getProductState()));

		// Setting the result
		data.setPrice(aggPrices);

		// Setting if has an occasion offer
	}

	
	/**
	 * Compute the estimated contribution for the given price
	 * @param price
	 * @return
	 */
	private Double computeEstimatedContribution(Double price) {
		return price * averageAffiliationRatio * incomesToBenefitsRatio * percentBenefitsReversed;
	}

    /**
     * Compute the price history for a given product condition.
     * 
     * @param prices the aggregated prices data
     * @param state  the product condition (NEW or OCCASION)
     */
    private void computePriceHistory(AggregatedPrices prices, ProductCondition state) {
    	
    	// TODO(p1, migration)  : put back when sanisation done
        AggregatedPrice minPrice = prices.getMinPrice(state).orElse(null);
//        if (minPrice.isEmpty()) {
//            return; // Exit if no minimum price is found for the condition
//        }

        ////////////////////
        // Normalization
        // Ensure only the lowest price is kept per day
        // TODO (P1,perf): Remove when migration ok
        ////////////////////
        Map<Long, PriceHistory> dailyLowestPrices = new HashMap<>();
        for (PriceHistory ph : prices.getHistory(state)) {

            // Check if we have a price for the current day
            dailyLowestPrices.merge(ph.getDay(), ph, (existing, newPrice) -> 
                existing.getPrice() <= newPrice.getPrice() ? existing : newPrice);
        }

        
        // Update the history with normalized prices
        List<PriceHistory> history = new ArrayList<>(dailyLowestPrices.values());
        history.sort(Comparator.comparingLong(PriceHistory::getTimestamp)); // Sort by timestamp to maintain order
        

        
        /*
         * We conserve one history price a day
         */
        if (minPrice != null ) {
        	if (history.size() ==  0 || !history.getLast().getPrice().equals(minPrice.getPrice())) {
        		history.add(new PriceHistory(minPrice));        			
        	}
        	else {        	
	        	PriceHistory lastPrice = history.getLast();
	        	if (null != lastPrice) {
	        		
	        		
	        		if (lastPrice.getDay() ==  System.currentTimeMillis() / (1000 * 60 * 60 * 24)) {
	        			history.removeLast();
	        		} 
	        		
	        	}
        	}
        }

        // Set the trend based on the last two price history values if the minimum price timestamp matches today's date
        if (history.size() >= 2) {
    		int trend = 0;
            PriceHistory secondLastPrice = history.get(history.size() - 2);
            PriceHistory lastPrice = history.get(history.size() - 1);

            if (lastPrice.getPrice() < secondLastPrice.getPrice()) {
                trend = -1; // Price decreased
            } else if (lastPrice.getPrice() > secondLastPrice.getPrice()) {
                trend = 1; // Price increased
            }
            prices.setTrend(trend);
        } else {
            prices.setTrend(0); // No recent price change or only one historical price
        }

       

        // TODO(p3,conf) : price history max size From conf
        LocalDate twoYearsAgoDate = LocalDate.now().minusYears(2);
        Instant twoYearsAgo = twoYearsAgoDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Remove entries with a price of 0.0 (invalid data) and entries older than 2 years
        history = history.stream()
            .filter(e -> e.getPrice() != 0.0) // Retain only valid price entries
            .filter(e -> Instant.ofEpochMilli(e.getTimestamp()).isAfter(twoYearsAgo)) // Retain entries within the last 2 years
            .collect(Collectors.toList());

        // Update the price history based on the product condition
        switch (state) {
            case NEW:
                prices.setNewPricehistory(history);
                break;
            case OCCASION:
                prices.setOccasionPricehistory(history);
                break;
        }
    }


	/**
	 * Add a key for a aprice and a marketplace.
	 * 
	 * @param df
	 * @return
	 */
	private String pricMerchanteKey(final AggregatedPrice df) {
		// Not hashing offerdame bypass the sellers (nice for min price logic)
		return df.getDatasourceName() + (null == df.getProductState() ? "" : df.getProductState());
	}

	/**
	 * Computes the actual prices (min, max, average)
	 *
	 * @param filtered
	 * @param p
	 */
	private void computeMinPrice(final Collection<AggregatedPrice> filtered, final AggregatedPrices p) {

		// Resetting the min price
		p.setMinPrice(null);
		// Min / max
		for (final AggregatedPrice o : filtered) {

			if (null == p.getMinPrice()) {
				p.setMinPrice(o);
			}
			if (o.lowerThan(p.getMinPrice())) {
				p.setMinPrice(o);
			}
		}
	}

	public @Override void close() throws IOException {
	}


	

}

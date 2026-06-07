
package org.open4goods.api.services.aggregation.services.realtime;

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

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Aggregates pricing information from all contributing {@link DataFragment}s
 * into a consolidated {@link AggregatedPrices} structure on the product.
 *
 * <p>Processing steps:
 * <ol>
 *   <li>Add the fragment's price to the product's offer set.</li>
 *   <li>Filter out offers older than {@code ProductRepository.VALID_UNTIL_DURATION}.</li>
 *   <li>Deduplicate by keeping only the lowest price per
 *       (datasource, product-state) pair.</li>
 *   <li>Compute the estimated nudger contribution for each offer.</li>
 *   <li>Update min-price, offer count, and available product conditions.</li>
 *   <li>Maintain and trim a per-condition price history (max 2 years,
 *       one entry per day, computed trends).</li>
 * </ol>
 *
 * <p>TODO(p3,conf): The price-history maximum size (currently 2 years) should
 * be driven by vertical configuration rather than hard-coded.
 */
public class PriceAggregationService extends AbstractAggregationService {

	/** Average percent reversed to Nudger by affiliation platforms. */
	private static final double averageAffiliationRatio = 0.05;

	/** Ratio used to estimate benefits from revenue. */
	private static final double incomesToBenefitsRatio = 0.75;

	/** Percent of benefits reversed to the collective. */
	private static final double percentBenefitsReversed = 0.1;


	public PriceAggregationService(final Logger logger) {
		super(logger);
	}

	@Override
	public void onDataFragment(final DataFragment fragment, final Product aggregatedData, final VerticalConfig vConf) throws AggregationSkipException {

		if (!fragment.hasPrice()) {
			dedicatedLogger.warn("No price for data fragment {}, skipping", fragment);
		} else if (fragment.getPrice().getPrice() == 0.0) {
			dedicatedLogger.info("Price is 0 for datafragment {}, skipping", fragment);
		} else {
			AggregatedPrice aggPrice = new AggregatedPrice(fragment);
			aggregatedData.getPrice().getOffers().add(aggPrice);
		}

		onProduct(aggregatedData, vConf);
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
				// Keep only the lowest price per (provider, condition) pair
				reducedPrices.merge(key, df, (existing, incoming) ->
						incoming.getPrice() < existing.getPrice() ? incoming : existing);
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

        AggregatedPrice minPrice = prices.getMinPrice(state).orElse(null);
        // Skip history computation when no price is available for the condition
        if (minPrice == null) {
            return;
        }

        // Ensure only the lowest price is kept per day (deduplicates history that may
        // contain multiple entries for the same day from earlier ingestion runs).
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
         * Maintain one price entry per day. If the price changed, append a new entry.
         * If the price is unchanged but we already have an entry for today, replace it
         * to refresh the timestamp rather than leaving a gap.
         */
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
        if (history.isEmpty() || !history.getLast().getPrice().equals(minPrice.getPrice())) {
            history.add(new PriceHistory(minPrice));
        } else {
            PriceHistory lastEntry = history.getLast();
            if (lastEntry.getDay() == today) {
                // Refresh today's entry with the current timestamp
                history.removeLast();
                history.add(new PriceHistory(minPrice));
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

            prices.getTrends().put(state, trend);

        } else {
        	 prices.getTrends().put(state, 0);
        }



        // TODO(p3,conf) : price history max size From conf
        LocalDate twoYearsAgoDate = LocalDate.now().minusYears(2);
        Instant twoYearsAgo = twoYearsAgoDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Remove entries with a price of 0.0 (invalid data) and entries older than 2 years
        history = history.stream()
            .filter(e -> e.getPrice() != 0.0) // Retain only valid price entries
            .filter(e -> Instant.ofEpochMilli(e.getTimestamp()).isAfter(twoYearsAgo)) // Retain entries within the last 2 years
            .toList();

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
		return df.getDatasourceName() + (df.getProductState() == null ? "" : df.getProductState());
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

			if (p.getMinPrice() == null) {
				p.setMinPrice(o);
			}
			if (o.lowerThan(p.getMinPrice())) {
				p.setMinPrice(o);
			}
		}
	}

}

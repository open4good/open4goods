
package org.open4goods.api.services.aggregation.services.realtime;

import java.io.IOException;
import java.util.*;

import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.ui.VerticalProperties;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.model.product.AggregatedPrices;
import org.open4goods.model.product.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.services.DataSourceConfigService;

import com.google.common.collect.Sets;

/**
 * This service compute price infos from DataFragments computations if not in
 * stock
 *
 *
 * TODO : Could cut the price history after a number / delay of history for
 * elastic size purpose
 *
 * @author goulven
 *
 */
public class PriceAggregationService extends AbstractRealTimeAggregationService {

	// TODO(conf, P3, 0.25) : from conf
	private static final double REVERSEMENT = 0.2;

	private DataSourceConfigService datasourceConfigService;

	private VerticalProperties segmentProperties;

	public PriceAggregationService(final String logsFolder, DataSourceConfigService datasourceConfigService,
			VerticalProperties segmentProperties, boolean toConsole) {
		super(logsFolder, toConsole);
		this.datasourceConfigService = datasourceConfigService;
		this.segmentProperties = segmentProperties;
	}

	@Override
	public void onDataFragment(final DataFragment e, final Product aggregatedData) {

		if (!e.hasPrice() || !e.affiliated()) {
			return;
		}

		AggregatedPrice aggPrice = new AggregatedPrice(e);

		///////////////////
		// Filtering : keeping lowest prices per provider and offer names
		//////////////////

		// Key is providerName + name
		final Map<String, AggregatedPrice> reducedPrices = new HashMap<>();

		// Adding current price in the list of all prices
		aggregatedData.getPrice().getOffers().add(aggPrice);

		for (final AggregatedPrice df : aggregatedData.getPrice().getOffers()) {

			// TODO : compute price history
			if (System.currentTimeMillis() - toMs(segmentProperties.getPriceValidity()) > df.getTimeStamp()) {
				dedicatedLogger.info("price too old for CSV datafragment {}, removing it", df);

			} else {

				final String key = pricMerchanteKey(df);

				if (null == reducedPrices.get(key) || reducedPrices.get(key).getPrice() > df.getPrice()) {
					reducedPrices.put(key, df);
				}
			}
		}

		final Set<AggregatedPrice> filtered = new HashSet<>(reducedPrices.values());

		////////////////////////////
		// Computing the compensation
		////////////////////////////

		for (AggregatedPrice price : filtered) {

			double percent;
			try {
				percent = datasourceConfigService.getDatasourceConfig(price.getDatasourceName()).getReversement()
						.doubleValue();
			} catch (Exception e1) {
				dedicatedLogger.info("Cannot get reversement for datasource {}", price.getDatasourceName());
				percent = 2.0;
			}

			price.setCompensation(price.getPrice() * (percent / 100) * REVERSEMENT);

		}

		AggregatedPrices aggPrices = aggregatedData.getPrice();

		/////////////////////////
		// Prices computation
		////////////////////////

		// Compute current prices
		computeMinPrice(filtered, aggPrices);

		// set Number of offers
		aggregatedData.setOffersCount(reducedPrices.size());
		
		// Reseting prices (reducing)
		aggregatedData.getPrice().setOffers(Sets.newHashSet(reducedPrices.values()));

		// Computing / incrementing history
		computePriceHistory(aggPrices, ProductState.OCCASION);
		computePriceHistory(aggPrices, ProductState.NEW);

		// Setting the product state summary
		aggPrices.getOffers().forEach(i -> aggPrices.getConditions().add(i.getProductState()));

		// Setting the result
		aggregatedData.setPrice(aggPrices);

		// Setting if has an occasion offer

	}

	/**
	 * Compute the price history
	 * 
	 * @param prices
	 * @param state
	 */
	private void computePriceHistory(AggregatedPrices prices, ProductState state) {
		Optional<AggregatedPrice> oMinPrice = prices.getMinPrice(state);
		if (oMinPrice.isEmpty()) {
			return;
		}

		AggregatedPrice minPrice = oMinPrice.get();

		List<PriceHistory> history = prices.getHistory(state);

		if (history.size() == 0) {
			// First price
			prices.setTrend(0);
			history.add(new PriceHistory(minPrice));

		} else {
			PriceHistory lastPrice = history.get(history.size() - 1);
			if (minPrice.getPrice() == lastPrice.getPrice().doubleValue()) {
				prices.setTrend(0);
				lastPrice.setTimestamp(System.currentTimeMillis());
			} else if (minPrice.getPrice() > lastPrice.getPrice().doubleValue()) {
				// Price has increased
				prices.setTrend(1);
				// TODO : Cut here to a fixed history
				history.add(new PriceHistory(minPrice));
			} else {
				// Price has decreased
				prices.setTrend(-1);
				// TODO : Cut here to a fixed history
				history.add(new PriceHistory(minPrice));
			}
		}
		
		
		// Setting
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

	/**
	 * Convert days to ms
	 *
	 * @param webPriceValidity
	 * @return
	 */
	private long toMs(Integer days) {
		return days * 24 * 3600 * 1000L;
	}

}


package org.open4goods.aggregation.services.aggregation;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalProperties;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.model.product.AggregatedPrices;
import org.open4goods.model.product.PriceHistory;
import org.open4goods.services.DataSourceConfigService;

/**
 * This service compute price infos from DataFragments computations if not in
 * stock
 *
 * @author goulven
 *
 */
public class PriceAggregationService extends AbstractAggregationService {

	// TODO(conf, P3, 0.25) : from conf
	private static final double REVERSEMENT = 0.2;

	private DataSourceConfigService datasourceConfigService;

	private VerticalProperties segmentProperties;

	public PriceAggregationService(final String logsFolder, DataSourceConfigService datasourceConfigService,
			VerticalProperties segmentProperties) {
		super(logsFolder);
		this.datasourceConfigService = datasourceConfigService;
		this.segmentProperties = segmentProperties;
	}

	@Override
	public void onDataFragment(final DataFragment e, final AggregatedData aggregatedData) {

	
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

				final String key = priceKey(df);

				if (null == reducedPrices.get(key) || reducedPrices.get(key).getPrice() > df.getPrice()) {
					reducedPrices.put(key, df);
				}
			}
		}

		final Set<AggregatedPrice> filtered = reducedPrices.values().stream().collect(Collectors.toSet());

		////////////////////////////
		// Computing the compensation
		////////////////////////////

		for (AggregatedPrice price : filtered) {

			if (price.isAffiliated()) {

				double percent;
				try {
					percent = datasourceConfigService.getDatasourceConfig(price.getDatasourceName()).getReversement()
							.doubleValue();
				} catch (Exception e1) {
					dedicatedLogger.info("Cannot get reversement for datasource {}", price.getDatasourceName());
					percent = 3.0;
				}

				price.setCompensation(price.getPrice() * (percent / 100) * REVERSEMENT);
			} else {
				price.setCompensation(0.0);
			}

		}

		AggregatedPrices aggPrices = new AggregatedPrices();
		
		aggPrices.setOffers(filtered);

		/////////////////////////
		// Prices computation
		////////////////////////

		// Compute current prices
		computePrices(filtered, aggPrices);

		
		// Number of offers
		aggregatedData.setOffersCount(reducedPrices.size());

		
		// Computing / incrementing history
		AggregatedPrice minPrice = aggPrices.getMinPrice();
		if (null != minPrice && minPrice.getProductState().equals(ProductState.NEW)) {
			
			
			
			if (aggPrices.getHistory().size() == 0 ) {
				// First price
				aggPrices.setTrend(0);
				aggPrices.getHistory().add(new PriceHistory(minPrice));
				
			}
			else {
				PriceHistory lastPrice = aggPrices.getHistory().get(aggPrices.getHistory().size()-1);
				if (minPrice.getPrice() == lastPrice.getPrice().doubleValue()) {
					aggPrices.setTrend(0);
				} else if (minPrice.getPrice() > lastPrice.getPrice().doubleValue() ) {
					// Price has increased
					aggPrices.setTrend(1);
					aggPrices.getHistory().add(new PriceHistory(minPrice));
				} else {
					// Price has decreased
					aggPrices.setTrend(-1);
				}
			}
			
		}
		
		
		// Setting the result
		aggregatedData.setPrice(aggPrices);

	}

	private String priceKey(final AggregatedPrice df) {
		return df.getDatasourceName() + (null == df.getProductState() ? "" : df.getProductState()) +  (df.getOfferName()== null ? "" : df.getOfferName().trim().toUpperCase());
	}

	/**
	 * Computes the actual prices (min, max, average)
	 * 
	 * @param filtered
	 * @param p
	 */
	private void computePrices(final Collection<AggregatedPrice> filtered, final AggregatedPrices p) {
		Double total = 0.0;
		Integer count = 0;

		// Min / max
		for (final AggregatedPrice o : filtered) {

			total += o.getPrice();
			count++;
			if (null == p.getMinPrice()) {
				p.setMinPrice(o);
			}

//			if (null == p.getMaxPrice()) {
//				p.setMaxPrice(o);
//			}
//
//			if (o.greaterThan(p.getMaxPrice())) {
//				p.setMaxPrice(o);
//			}

			if (o.lowerThan(p.getMinPrice())) {
				p.setMinPrice(o);
			}
		}

		// Average
//		if (count != 0) {
//			p.setAvgPrice(new AggregatedPrice(total / count, StandardiserService.DEFAULT_CURRENCY));
//		}
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

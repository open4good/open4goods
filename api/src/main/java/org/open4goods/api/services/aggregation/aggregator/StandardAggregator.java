package org.open4goods.api.services.aggregation.aggregator;

import java.util.List;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Realtime aggregator that chains {@link AbstractAggregationService} instances
 * to enrich a {@link Product} either from an incoming {@link DataFragment} or
 * from the product's accumulated state alone.
 *
 * <p>Two entry points are provided:
 * <ul>
 *   <li>{@link #onDatafragment(DataFragment, Product)} — called when a new
 *       merchant offer arrives; delegates to each service's
 *       {@code onDataFragment} hook.</li>
 *   <li>{@link #onProduct(Product)} — called during batch sanitisation; delegates
 *       to each service's {@code onProduct} hook, which re-derives computed
 *       fields from the product's aggregated data without a fragment input.</li>
 * </ul>
 *
 * <p>Both methods look up the vertical configuration once per product and pass it
 * to every service to avoid redundant lookups inside the loop.
 */
public class StandardAggregator extends AbstractAggregator {

	protected static final Logger logger = LoggerFactory.getLogger(StandardAggregator.class);

	private final VerticalsConfigService verticalConfigService;

	/**
	 * @param services             ordered list of aggregation services to apply
	 * @param verticalConfigService service used to resolve per-vertical configuration
	 */
	public StandardAggregator(final List<AbstractAggregationService> services,
			final VerticalsConfigService verticalConfigService) {
		super(services);
		this.verticalConfigService = verticalConfigService;
	}

	/**
	 * Enriches {@code data} with the content of the incoming {@code fragment},
	 * running every registered service in sequence.
	 *
	 * <p>An {@link AggregationSkipException} thrown by any service is re-thrown
	 * immediately, aborting processing of the remaining services. Any other
	 * exception is caught, logged, and processing continues with the next service.
	 *
	 * @param fragment incoming data fragment (a single merchant offer)
	 * @param data     product being built; modified in-place
	 * @return {@code data} for chaining convenience
	 * @throws AggregationSkipException if a service requests that this product be skipped
	 */
	public Product onDatafragment(final DataFragment fragment, final Product data) throws AggregationSkipException {

		logger.debug("Incrementing Product with {} DataFragment and using {} services", fragment, services.size());

		VerticalConfig vConf = verticalConfigService.getConfigByIdOrDefault(data.getVertical());
		for (final AbstractAggregationService service : services) {
			try {
				service.onDataFragment(fragment, data, vConf);
			} catch (AggregationSkipException e) {
				throw e;
			} catch (final Exception e) {
				logger.error("AggregationFacadeService {} threw an exception while processing data {}",
						service.getClass().getName(), data, e);
			}
		}
		return data;
	}

	/**
	 * Re-derives computed product fields without a fragment input, running every
	 * registered service's {@code onProduct} hook in sequence.
	 *
	 * <p>Used during batch sanitisation to re-apply aggregation logic to products
	 * already stored in Elasticsearch.
	 *
	 * @param data product to update; modified in-place
	 * @return {@code data} for chaining convenience
	 * @throws AggregationSkipException if a service requests that this product be skipped
	 */
	public Product onProduct(final Product data) throws AggregationSkipException {

		logger.debug("Updating product using {} services", services.size());

		VerticalConfig vConf = verticalConfigService.getConfigByIdOrDefault(data.getVertical());
		for (final AbstractAggregationService service : services) {
			try {
				service.onProduct(data, vConf);
			} catch (AggregationSkipException e) {
				throw e;
			} catch (final Exception e) {
				logger.error("AggregationFacadeService {} threw an exception while processing data {}",
						service.getClass().getName(), data, e);
			}
		}
		return data;
	}
}

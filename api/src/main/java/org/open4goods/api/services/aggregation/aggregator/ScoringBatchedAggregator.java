package org.open4goods.api.services.aggregation.aggregator;

import java.util.Collection;
import java.util.List;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch-mode aggregator that drives the scoring pipeline for a collection of products.
 *
 * <p>For each registered {@link AbstractAggregationService}, the aggregator runs the
 * full lifecycle in order:
 * <ol>
 *   <li>{@link AbstractAggregationService#init(Collection)} — reset cardinalities and state</li>
 *   <li>{@link AbstractAggregationService#onProduct(Product, VerticalConfig)} for every product</li>
 *   <li>{@link AbstractAggregationService#done(Collection, VerticalConfig)} — post-compute
 *       (relativisation, ranking, etc.)</li>
 * </ol>
 *
 * <p>Services are executed sequentially; each service's {@code done()} output is visible
 * to the next service's {@code init()}.
 */
public class ScoringBatchedAggregator extends AbstractAggregator {

	protected static final Logger logger = LoggerFactory.getLogger(ScoringBatchedAggregator.class);

	/**
	 * Creates a new aggregator with the given ordered list of scoring services.
	 *
	 * @param services ordered list of {@link AbstractAggregationService} implementations
	 */
	public ScoringBatchedAggregator(final List<AbstractAggregationService> services) {
		super(services);
	}

	/**
	 * Runs every registered scoring service through its full lifecycle against the
	 * provided product collection.
	 *
	 * @param datas products to score; modified in-place
	 * @param vConf vertical configuration driving score definitions and weights
	 * @throws AggregationSkipException propagated when a service requests that a product be skipped
	 */
	public void score(final Collection<Product> datas, final VerticalConfig vConf) throws AggregationSkipException {

		for (final AbstractAggregationService service : services) {

			logger.info("Batching {} products using {} service", datas.size(), service.getClass().getSimpleName());

			service.init(datas);

			logger.info("Processing {} products using {} service", datas.size(), service.getClass().getSimpleName());

			for (Product p : datas) {
				service.onProduct(p, vConf);
			}

			logger.info("Post computing {} products using {} service", datas.size(), service.getClass().getSimpleName());

			service.done(datas, vConf);
		}
	}

}

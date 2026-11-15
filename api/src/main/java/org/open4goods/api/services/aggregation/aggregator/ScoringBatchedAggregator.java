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

	private static final Logger logger = LoggerFactory.getLogger(ScoringBatchedAggregator.class);

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
	 * <p>An {@link AggregationSkipException} thrown for one product is logged and skips that
	 * product for that service only: the remaining products and services continue.
	 *
	 * <p>Any other exception stops the whole scoring run, on purpose: services such as
	 * {@code CleanScoreAggregationService} clear the scores in {@code init}, so going on would
	 * leave products without scores. The id of the product and the name of the service are
	 * logged, then the exception is rethrown; {@code BatchService} then indexes nothing for the
	 * vertical (GOU-2273).
	 *
	 * @param datas products to score; modified in-place
	 * @param vConf vertical configuration driving score definitions and weights
	 */
	public void score(final Collection<Product> datas, final VerticalConfig vConf) {

		int count = datas.size();
		for (final AbstractAggregationService service : services) {

			logger.info("Batching {} products using {} service", count, service.getClass().getSimpleName());

			service.init(datas);

			logger.info("Processing {} products using {} service", count, service.getClass().getSimpleName());

			for (Product p : datas) {
				try {
					service.onProduct(p, vConf);
				} catch (AggregationSkipException e) {
					logger.info("Product {} skipped during batch scoring by {}: {}",
							p.getId(), service.getClass().getSimpleName(), e.getMessage());
				} catch (RuntimeException e) {
					// Callers log the stack trace
					logger.error("Scoring of product {} failed in {}, the scoring run stops: {}",
							p.getId(), service.getClass().getSimpleName(), e.toString());
					throw e;
				}
			}

			logger.info("Post computing {} products using {} service", count, service.getClass().getSimpleName());

			service.done(datas, vConf);
		}
	}

}

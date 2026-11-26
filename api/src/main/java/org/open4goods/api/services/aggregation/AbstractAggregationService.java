package org.open4goods.api.services.aggregation;

import java.util.Collection;

import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Contract for a single step in the aggregation pipeline.
 *
 * <p>Each implementation is responsible for one cross-cutting concern
 * (identity validation, taxonomy mapping, attribute parsing, pricing, etc.).
 * Services are chained by {@link org.open4goods.api.services.aggregation.aggregator.AbstractAggregator}
 * subclasses and executed in registration order.
 *
 * <p>Lifecycle overview:
 * <ol>
 *   <li>{@link #init(Collection)} — batch scoring only: called once before the products of a
 *       vertical are scored, to reset run-scoped state (cardinalities, etc.).</li>
 *   <li>{@link #onDataFragment(DataFragment, Product, VerticalConfig)} — called for
 *       each incoming {@link DataFragment} in realtime mode.</li>
 *   <li>{@link #onProduct(Product, VerticalConfig)} — called to (re-)derive product-level
 *       fields from the product's aggregated state; used both in the realtime path
 *       (after {@code onDataFragment}) and standalone during batch sanitisation.</li>
 *   <li>{@link #done(Collection, VerticalConfig)} — batch scoring only: called once after all
 *       products have been scored, for post-compute steps (relativisation, ranking).</li>
 * </ol>
 *
 * <p>The services of the standard chain are built once and shared by the realtime ingestion,
 * the completion threads and the batch (GOU-2280): they must hold no mutable state. The scoring
 * services hold run-scoped state and are built again for each scoring run.
 */
public abstract class AbstractAggregationService {

	/** Logger dedicated to the aggregation log channel (separate file from the application log). */
	protected final Logger dedicatedLogger;

	/**
	 * @param logger dedicated aggregation logger supplied by the factory
	 */
	public AbstractAggregationService(final Logger logger) {
		this.dedicatedLogger = logger;
	}

	/**
	 * Enriches {@code output} with the content of the incoming {@code input} fragment.
	 *
	 * <p>Default implementation is a no-op; override when the service needs to
	 * process raw merchant data.
	 *
	 * @param input  incoming data fragment
	 * @param output product being built; modified in-place
	 * @param vConf  vertical configuration for the product's category
	 * @throws AggregationSkipException if this product should be skipped entirely
	 */
	public void onDataFragment(final DataFragment input, final Product output,
			final VerticalConfig vConf) throws AggregationSkipException {
	}

	/**
	 * Re-derives computed product fields from the product's accumulated state.
	 *
	 * <p>Called after {@link #onDataFragment} in the realtime path, and directly
	 * during batch sanitisation. Implementations should be idempotent.
	 *
	 * @param data  product to update; modified in-place
	 * @param vConf vertical configuration for the product's category
	 * @throws AggregationSkipException if this product should be skipped entirely
	 */
	public abstract void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException;

	/**
	 * Resets run-scoped state before batch processing starts.
	 *
	 * <p>Default implementation is a no-op. Override to clear cardinalities,
	 * frequency maps, caches, or any other per-batch accumulator.
	 *
	 * @param datas full collection of products about to be processed
	 */
	public void init(final Collection<Product> datas) {
	}

	/**
	 * Performs post-compute steps after all products have been processed in batch mode.
	 *
	 * <p>Default implementation is a no-op. Override to relativise scores, compute
	 * rankings, or flush buffers that depend on the full batch statistics.
	 *
	 * @param datas all products that were processed
	 * @param vConf vertical configuration for this batch
	 */
	public void done(final Collection<Product> datas, final VerticalConfig vConf) {
	}

}

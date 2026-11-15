package org.open4goods.api.services.aggregation.aggregator;

import java.util.List;

import org.open4goods.api.services.aggregation.AbstractAggregationService;

/**
 * Base class for aggregators that orchestrate an ordered list of
 * {@link AbstractAggregationService} instances to enrich or score
 * {@code Product} objects.
 *
 * <p>Concrete subclasses define the processing mode (standard chain vs. batch scoring)
 * and the iteration strategy.
 *
 * <p>Aggregators and their services are plain objects built with {@code new} by
 * {@code AggregationFacadeService}, which hands each service its collaborators through its
 * constructor: there is no Spring injection into them, and no lifecycle to start or close
 * (GOU-2280).
 */
public abstract class AbstractAggregator {

	/** Ordered, unmodifiable list of services applied to each product or data-fragment. */
	protected final List<AbstractAggregationService> services;

	/**
	 * @param services ordered list of aggregation services to chain
	 */
	protected AbstractAggregator(final List<AbstractAggregationService> services) {
		this.services = List.copyOf(services);
	}

}

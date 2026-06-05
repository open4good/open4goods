package org.open4goods.api.services.aggregation.aggregator;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Base class for aggregators that orchestrate an ordered list of
 * {@link AbstractAggregationService} instances to enrich or score
 * {@code Product} objects.
 *
 * <p>Concrete subclasses define the processing mode (realtime vs. batch-scoring)
 * and the iteration strategy. This class provides lifecycle support for Spring
 * autowiring ({@link #beforeStart()}) and resource cleanup ({@link #close()}).
 *
 * <p>Aggregator instances are created programmatically by
 * {@code AggregationFacadeService} rather than as Spring beans, because each
 * invocation mode requires a fresh, independent service chain. Spring injection
 * into services is performed post-construction via
 * {@link AutowireCapableBeanFactory#autowireBean(Object)}.
 */
public abstract class AbstractAggregator implements Closeable {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractAggregator.class);

	@Autowired
	private AutowireCapableBeanFactory autowireBeanFactory;

	/** Ordered list of services applied to each product or data-fragment. */
	protected final List<AbstractAggregationService> services;

	/**
	 * @param services ordered list of aggregation services to chain
	 */
	public AbstractAggregator(final List<AbstractAggregationService> services) {
		this.services = services;
	}

	/**
	 * Prepares the service chain before processing starts: performs Spring field
	 * injection on every service, then calls {@link AbstractAggregationService#init}
	 * with an empty collection to reset any transient state.
	 *
	 * <p>Called by completion services (Wikidata, Icecat, Amazon, EPREL) that
	 * reuse a {@link StandardAggregator} outside the main batch pipeline.
	 */
	public void beforeStart() {
		for (AbstractAggregationService s : services) {
			autowireBeanFactory.autowireBean(s);
		}
		for (AbstractAggregationService s : services) {
			s.init(new HashSet<>());
		}
	}

	/**
	 * Releases resources held by each service (file handles, thread pools, etc.).
	 * Errors are logged but do not prevent subsequent services from being closed.
	 */
	@Override
	public void close() {
		for (final AbstractAggregationService s : services) {
			try {
				s.close();
			} catch (IOException e) {
				logger.error("Closing aggregation service failed", e);
			}
		}
	}

}

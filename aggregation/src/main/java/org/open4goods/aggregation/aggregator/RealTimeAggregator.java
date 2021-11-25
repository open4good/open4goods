package org.open4goods.aggregation.aggregator;

import java.util.List;

import org.open4goods.aggregation.AbstractAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The real time AggregationService is stateless and thread safe.
 * @author goulven
 *
 */
public class RealTimeAggregator extends AbstractAggregator {

	protected static final Logger logger = LoggerFactory.getLogger(RealTimeAggregator.class);


	public RealTimeAggregator(final List<AbstractAggregationService> services) {
		super(services);
	}

}

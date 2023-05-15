package org.open4goods.aggregation.aggregator;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.open4goods.aggregation.AbstractAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Orchestration class that contains and orchestrate AggregationServices against a set of FragmentData to obtain an AggregatedData
 * @author goulven
 *
 */

public abstract class AbstractAggregator implements Closeable{


	protected static final Logger logger = LoggerFactory.getLogger(AbstractAggregator.class);


	@Autowired
	private AutowireCapableBeanFactory autowireBeanFactory;

	/**
	 *
	 * The ordered list of aggregation services that will be involved in the AggragatedData computation
	 */
	protected  List<AbstractAggregationService> services;


	public AbstractAggregator (final List<AbstractAggregationService> services) {
		this.services = services;
	}


	public void beforeStart() {
		// Auto wiring spring components
		services.stream().forEach((s) -> {autowireBeanFactory.autowireBean(s); }) ;
		
		// Calling init
		services.stream().forEach(s -> {s.init();}) ;

	}
	

	
	


	@Override
	public void close() {
		for (final AbstractAggregationService s : services) {
			try {
				s.close();
			} catch (IOException e) {
				logger.error("Closing aggregation service failed",e);
			}
		}
	}



}

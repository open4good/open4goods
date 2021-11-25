package org.open4goods.aggregation.aggregator;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.exceptions.NotAddedException;
import org.open4goods.model.aggregation.ParticipantData;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import io.micrometer.core.annotation.Timed;

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
	/**
	 * Build the AggregatedData using the services registered on this aggregator
	 * @param datas
	 * @return
	 */
	@Timed(value="AbstractAggregator.build()",description="Building an aggregated data from DataFragments")
	public AggregatedData build(final DataFragment fragment, final AggregatedData data ) throws NotAddedException {

		logger.info("Incrementing AggregatedData with {} DataFragment and using {} services",fragment,services.size());

		// Call transformation building registered service
		for (final AbstractAggregationService service : services) {

			try {
				service.onDataFragment(fragment, data);

			}
			catch (final Exception e) {
				logger.warn("AggregationService {} throw an exception while processing data {}",service.getClass().getName(), data,e);

			}
		}

		// Computing the participant data
		final ParticipantData pd = new ParticipantData();
		pd.setDataUrl(fragment.affiliatedUrlIfPossible());
		pd.setProviderName(fragment.getDatasourceConfigName());

	
		data.getAggregationResult().getParticipantDatas().add(pd);

		return data;
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

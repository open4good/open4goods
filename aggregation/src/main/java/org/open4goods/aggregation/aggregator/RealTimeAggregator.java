package org.open4goods.aggregation.aggregator;

import java.util.List;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.annotation.Timed;

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

	
	/**
	 * Build the AggregatedData using the services registered on this aggregator
	 * @param datas
	 * @return
	 * @throws AggregationSkipException 
	 */
	@Timed(value="AbstractAggregator.build()",description="Building an aggregated data from DataFragments")
	public AggregatedData build(final DataFragment fragment, final AggregatedData data ) throws AggregationSkipException {

		logger.info("Incrementing AggregatedData with {} DataFragment and using {} services",fragment,services.size());

		// Call transformation building registered service
		for (final AbstractAggregationService service : services) {

			try {
				service.onDataFragment(fragment, data);

			}
			catch (AggregationSkipException e) {
				throw e;
			}
			catch (final Exception e) {
				logger.warn("AggregationService {} throw an exception while processing data {}",service.getClass().getName(), data,e);

			}
		}

		// Computing the participant data
//		final ParticipantData pd = new ParticipantData();
//		pd.setDataUrl(fragment.affiliatedUrlIfPossible());
//		pd.setProviderName(fragment.getDatasourceConfigName());

	
//		data.getAggregationResult().getParticipantDatas().add(pd);

		return data;
	}

}

package org.open4goods.api.services.aggregation.aggregator;

import java.util.List;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
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


	/**
	 * Build the Product using the services registered on this aggregator
	 * @param datas
	 * @return
	 * @throws AggregationSkipException
	 */
	public Product build(final DataFragment fragment, final Product data ) throws AggregationSkipException {

		logger.debug("Incrementing Product with {} DataFragment and using {} services",fragment,services.size());

		// Call transformation building registered service
		for (final AbstractAggregationService s : services) {

			AbstractRealTimeAggregationService service = null;
			if (s instanceof AbstractRealTimeAggregationService) {
				service = (AbstractRealTimeAggregationService)s;
			} else {
				logger.warn("Aggrgegator {} is used in realtime mode, but is a AbstractRealTimeAggregationService", s.getClass().getName());
				continue;
			}
			
			try {
				service.onDataFragment(fragment, data);

			}
			catch (AggregationSkipException e) {
				throw e;
			}
			catch (final Exception e) {
				e.printStackTrace();
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
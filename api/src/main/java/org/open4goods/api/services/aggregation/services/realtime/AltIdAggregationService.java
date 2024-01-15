package org.open4goods.api.services.aggregation.services.realtime;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AltIdAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(AltIdAggregationService.class);

	public AltIdAggregationService( final Logger logger) {
		super(logger);

	}

	@Override
	public void onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {

		// ID is defined in barcode aggregation service


		// Adding alternate id's
		output.getAlternativeIds().addAll(input.getAlternateIds());

		// The last update
		if ( output.getLastChange() < input.getLastIndexationDate()) {
			output.setLastChange(input.getLastIndexationDate());
		}
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
	}



}

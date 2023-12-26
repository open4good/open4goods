package org.open4goods.api.services.aggregation.services.realtime;

import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdAggregationService extends AbstractRealTimeAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(IdAggregationService.class);

	public IdAggregationService( final String logsFolder,boolean toConsole) {
		super(logsFolder, toConsole);

	}

	@Override
	public void onDataFragment(final DataFragment input, final Product output) {

		// ID is defined in barcode aggregation service


		// Adding alternate id's
		output.getAlternativeIds().addAll(input.getAlternateIds());

		// The last update
		if ( output.getLastChange() < input.getLastIndexationDate()) {
			output.setLastChange(input.getLastIndexationDate());
		}
	}



}

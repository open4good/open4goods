package org.open4goods.aggregation.services.aggregation;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.SiteNaming;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.GoogleTaxonomyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(IdAggregationService.class);

	public IdAggregationService( final String logsFolder) {
		super(logsFolder);

	}

	@Override
	public void onDataFragment(final DataFragment input, final AggregatedData output) {


		// Adding alternate id's
		output.getAlternativeIds().addAll(input.getAlternateIds());

		// The last update
		if (null == output.getLastChange()
				|| output.getLastChange().longValue() < input.getLastIndexationDate().longValue()) {
			output.setLastChange(input.getLastIndexationDate());
		}
	}

}

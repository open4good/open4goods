package org.open4goods.aggregation.services.aggregation;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.SiteNaming;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(IdAggregationService.class);

	private final SiteNaming localisationAggregationConfig;

	private final EvaluationService evaluationService;


	public IdAggregationService(final SiteNaming localisationAggregationConfig,
			final EvaluationService evaluationService, final String logsFolder) {
		super(logsFolder);
		this.localisationAggregationConfig = localisationAggregationConfig;
		this.evaluationService = evaluationService;
	}

	@Override
	public void onDataFragment(final DataFragment input, final AggregatedData output) {

		// The participating product tags
		output.getDatasourceCategories().add(input.getCategory());

		// Adding alternate id's
		output.getAlternativeIds().addAll(input.getAlternateIds());

		// The last update
		if (null == output.getLastChange()
				|| output.getLastChange().longValue() < input.getLastIndexationDate().longValue()) {
			output.setLastChange(input.getLastIndexationDate());
		}
	}

}

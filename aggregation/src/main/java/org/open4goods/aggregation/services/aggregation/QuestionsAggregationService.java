
package org.open4goods.aggregation.services.aggregation;

import java.util.Map;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;


public class QuestionsAggregationService extends AbstractAggregationService {

	public QuestionsAggregationService(final String logsFolder) {
		super(logsFolder);
	}


	public @Override void onDataFragment(final DataFragment input, final AggregatedData output) {
		output.getQuestions().addAll(input.getQuestions());
	}

}

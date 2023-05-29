//
//package org.open4goods.aggregation.services.aggregation;
//
//import org.open4goods.aggregation.AbstractAggregationService;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.product.Product;
//
//
//public class QuestionsAggregationService extends AbstractAggregationService {
//
//	public QuestionsAggregationService(final String logsFolder) {
//		super(logsFolder);
//	}
//
//
//	public @Override void onDataFragment(final DataFragment input, final Product output) {
//		output.getQuestions().addAll(input.getQuestions());
//	}
//
//}

package org.open4goods.aggregation.services.aggregation;

import java.util.Collection;

import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.model.product.Product;

public class CleanScoreAggregationService extends AbstractScoreAggregationService {

	private final AttributesConfig attributesConfig;

	public CleanScoreAggregationService(final AttributesConfig attributesConfig,  final String logsFolder,boolean toConsole) {
		super(logsFolder, toConsole);
		this.attributesConfig = attributesConfig;
	}



	@Override
	public void init(Collection<Product> datas) {
		super.init(datas);
		
		for (Product d : datas) {
			d.getScores().clear();
		}
	}
	
}

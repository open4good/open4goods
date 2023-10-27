package org.open4goods.aggregation.services.aggregation.batch;

import org.open4goods.aggregation.AbstractBatchAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class VerticalBatchedAggregationService extends AbstractBatchAggregationService {

	private VerticalsConfigService verticalService;

	public VerticalBatchedAggregationService( final String logsFolder, final VerticalsConfigService verticalService,boolean toConsole) {
		super(logsFolder, toConsole);
		this.verticalService = verticalService;

	}

	@Override
	public void onProduct(Product data) {
		// Getting the config for the category, if any

		VerticalConfig vConf = verticalService.getVerticalForCategories(data.getDatasourceCategories());

		if (null != vConf) {
			// We have a match. Associate vertical ID annd save
			data.setVertical(vConf.getId());

		} else if (null != data.getVertical() ){
			// No match, but a vertical already defined. Removing
			dedicatedLogger.warn("Nulling Vertical for {} ", data.bestName());
			data.setVertical(null);
		}
	}

}

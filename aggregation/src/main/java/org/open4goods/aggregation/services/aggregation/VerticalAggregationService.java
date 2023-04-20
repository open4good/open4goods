package org.open4goods.aggregation.services.aggregation;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.SiteNaming;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class VerticalAggregationService extends AbstractAggregationService {
	
	private VerticalsConfigService verticalService;

	public VerticalAggregationService( final String logsFolder, final VerticalsConfigService verticalService) {
		super(logsFolder);
		this.verticalService = verticalService;

	}

	@Override
	public void onDataFragment(final DataFragment input, final AggregatedData output) {

		VerticalConfig vertical = verticalService.getVerticalForCategoryName(input.getCategory());
		
		
		if (null != vertical) {
			if ( null != output.getVertical() && !vertical.getId().equals(output.getVertical())) {
				dedicatedLogger.warn("Will erase existing vertical {} with {} for product {}, because of category {}", output.getVertical(), vertical.getId(), output.bestName(), input.getCategory());
			} 			
			output.setVertical(vertical.getId());
			
		}		
	}

}

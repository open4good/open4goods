package org.open4goods.aggregation.services.aggregation;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;

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
	public void onDataFragment(final DataFragment input, final Product output) {

		String category = input.getCategory();
		
		
		if (!StringUtils.isBlank(category)) {
			
			output.getDatasourceCategories().add(category);
			
			// Adding vertical
			VerticalConfig vertical = verticalService.getVerticalForCategories(output.getDatasourceCategories());	
			
			if (null != vertical) {
				if ( null != output.getVertical() && !vertical.getId().equals(output.getVertical())) {
					dedicatedLogger.warn("Will erase existing vertical {} with {} for product {}, because of category {}", output.getVertical(), vertical.getId(), output.bestName(), input.getCategory());
				} 			
				output.setVertical(vertical.getId());
				
			}		
		} else {
			dedicatedLogger.warn("No category for {}", input);
		}

		// Setting no vertical if no category
		if (output.getDatasourceCategories().size() == 0) {
			dedicatedLogger.warn("No category in {}, removing vertical", output);
			output.setVertical(null);
		}
	
	}
}

package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyVal;
import org.open4goods.model.product.Product;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.VerticalsConfigService;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class TaxonomyRealTimeAggregationService extends AbstractRealTimeAggregationService {

	private VerticalsConfigService verticalService;
	private GoogleTaxonomyService taxonomyService;

	public TaxonomyRealTimeAggregationService( final String logsFolder, final VerticalsConfigService verticalService,GoogleTaxonomyService taxonomyService, boolean toConsole) {
		super(logsFolder, toConsole);
		this.verticalService = verticalService;
		this.taxonomyService = taxonomyService;

	}

	@Override
	public void onDataFragment(final DataFragment input, final Product output) {

		setVerticalFromCategories(input, output);
		
		Integer taxonomy =   googleTaxonomy(input);
		
		if (null != taxonomy) {			
			output.setGoogleTaxonomyId(taxonomy);
		}
	}

	
	
	/**
	 * Try to detect the google taxonomy id
	 * @param input
	 * @return 
	 */
	private Integer googleTaxonomy(final DataFragment input) {
		Integer taxonomyId = null;
		
		List<Integer> taxons =new ArrayList<>();

		//TODO : equivalent in a batch service, for stock processing
		input.getAttributes().forEach(a -> {
			String i = a.getName();
			
			if (i.contains("CATEGORY")) {
				Integer t = taxonomyService.resolve(a.getValue());
				if (null != t) {
					taxons.add(t);					
				}
			}			
		});
		
		if (taxons.size() == 1) {
			taxonomyId = taxons.stream().findAny().orElse(null);
		} else if (taxons.size() > 1) {
			// TODO : The language (should not be needed), will bug when other languages
			taxonomyId = taxonomyService.selectDeepest("fr", taxons);
		}
		
		return taxonomyId;
	}

	
	/**
	 * Defines a vertical and a taxonomy id from the config based matching
	 * @param input
	 * @param output
	 */
	private void setVerticalFromCategories(final DataFragment input, final Product output) {
		String category = input.getCategory();


		if (!StringUtils.isBlank(category)) {

			output.getDatasourceCategories().add(category);
			
					
			output.getMappedCategories().add(new UnindexedKeyVal(input.getDatasourceConfigName(), category));

			// Adding vertical
			VerticalConfig vertical = verticalService.getVerticalForCategories(output.getDatasourceCategories());

			if (null != vertical) {
				if ( null != output.getVertical() && !vertical.getId().equals(output.getVertical())) {
					dedicatedLogger.warn("Will erase existing vertical {} with {} for product {}, because of category {}", output.getVertical(), vertical.getId(), output.bestName(), input.getCategory());
				}
				output.setVertical(vertical.getId());

			}
		} else {
			dedicatedLogger.info("No category for {}", input);
		}

		// Setting no vertical if no category
		if (output.getDatasourceCategories().size() == 0) {
			dedicatedLogger.info("No category in {}, removing vertical", output);
			output.setVertical(null);
		}
	}

	@Override
	public void handle(Product output) throws AggregationSkipException {
		// TODO Auto-generated method stub
		
	}

}

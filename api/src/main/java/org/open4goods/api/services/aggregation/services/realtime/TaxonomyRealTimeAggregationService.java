package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyVal;
import org.open4goods.model.product.Product;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class TaxonomyRealTimeAggregationService extends AbstractAggregationService {

	private VerticalsConfigService verticalService;
	private GoogleTaxonomyService taxonomyService;

	public TaxonomyRealTimeAggregationService( final Logger logger,  final VerticalsConfigService verticalService,GoogleTaxonomyService taxonomyService) {
		super(logger);
		this.verticalService = verticalService;
		this.taxonomyService = taxonomyService;

	}

	@Override
	public void onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {

		
		String category = input.getCategory();
		if (!StringUtils.isEmpty(category)) {
			output.getDatasourceCategories().add(category);
			output.getMappedCategories().add(new UnindexedKeyVal(input.getDatasourceConfigName(), category));
		} 
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
		
		////////////////////////////
		// Setting vertical from category
		////////////////////////////
		VerticalConfig vertical = verticalService.getVerticalForCategories(data.getDatasourceCategories());
		if (null != vertical) {
			if ( null != data.getVertical() && !vertical.getId().equals(data.getVertical())) {
				dedicatedLogger.warn("Will erase existing vertical {} with {} for product {}, because of category {}", data.getVertical(), vertical.getId(), data.bestName());
			}
			data.setVertical(vertical.getId());

		}
		
		// Setting no vertical if no category
		if (data.getDatasourceCategories().size() == 0) {
			dedicatedLogger.info("No category in {}, removing vertical", data);
			data.setVertical(null);
		}
		
		
		////////////////////////////
		// Setting google taxonomy
		////////////////////////////
		data.setGoogleTaxonomyId(null);
		if (null != vertical &&  null != vertical.getTaxonomyId()) {
			data.setGoogleTaxonomyId(vertical.getTaxonomyId());
		} else {
			if (data.getDatasourceCategories().size() != 0) {
				Integer taxonomy =   googleTaxonomy(data);
				if (null != taxonomy) {			
					data.setGoogleTaxonomyId(taxonomy);
					dedicatedLogger.info("No taxonomy found for categories : {}", data.getDatasourceCategories());
					
				} else {
					dedicatedLogger.info("No taxonomy found for categories : {}", data.getDatasourceCategories());
				}
			}
		}		
	}
	
	
	/**
	 * Try to detect the google taxonomy id
	 * @param input
	 * @return 
	 */
	private Integer googleTaxonomy(final Product input) {
		Integer taxonomyId = null;
		
		List<Integer> taxons =new ArrayList<>();

		input.getAttributes().getUnmapedAttributes().forEach(a -> {
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

	


}

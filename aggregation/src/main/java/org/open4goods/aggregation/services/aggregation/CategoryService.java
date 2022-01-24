package org.open4goods.aggregation.services.aggregation;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.GoogleTaxonomyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
	
	private GoogleTaxonomyService taxonomyService;

	public CategoryService( final String logsFolder, GoogleTaxonomyService taxonomyService) {
		super(logsFolder);
		this.taxonomyService = taxonomyService;
	}

	@Override
	public void onDataFragment(final DataFragment input, final AggregatedData output) {
		
		// The participating product tags
		output.getDatasourceCategories().add(input.getCategory());
				
		//////////////////////////////
		// Resolving taxonomy id
		//////////////////////////////
		
		// Trying to resolve from google mapping		
		String cat = input.getProductCategory().trim();
		
		// Trying to resolve through google taxonomy
		//TODO : I18n
		Integer gCat = taxonomyService.getTaxonomyId("fr",input.getProductCategory());
		if (null != gCat) {
			dedicatedLogger.warn("cat {} resolved from Google mappings : {}", cat, gCat);
			output.getGoogleTaxonomyIds().add(gCat);
			return;			
		}
		
		// Trying to resolve from raw categories		
		gCat = taxonomyService.getRawTaxonomy(cat);
		if (null != gCat) {
			dedicatedLogger.warn("cat {} resolved from raw mappings : {}", cat, gCat);
			output.getGoogleTaxonomyIds().add(gCat);
			return;			
		}

		// Unsolvable category
		dedicatedLogger.error("Cannot resolve category : {}", cat);

		// Adding to stats
		taxonomyService.incrementUnmapped(cat);
		
		
	}

}

package org.open4goods.api.services.aggregation.services.batch;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.api.services.aggregation.AbstractBatchAggregationService;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class UnmappedAttributeCleaningBatchAggregationService extends AbstractBatchAggregationService {

	private VerticalsConfigService verticalConfigService;

	public UnmappedAttributeCleaningBatchAggregationService( final String logsFolder,  VerticalsConfigService verticalConfigService, boolean toConsole) {
		super(logsFolder, toConsole);
		this.verticalConfigService = verticalConfigService;
		
	}

	@Override
	public void onProduct(Product product) {


		
		try {
			
			AttributesConfig attributesConfig = verticalConfigService.getConfigById(product.getVertical() == null ? "all" : product.getVertical() ).get().getAttributesConfig() ;

			// Adding the list of "to be removed" attributes
			Set<String> toRemoveFromUnmatched = new HashSet<>(attributesConfig.getExclusions());
		
			// Removing 
			product.getAttributes().setUnmapedAttributes(product.getAttributes().getUnmapedAttributes().stream().filter(e -> !toRemoveFromUnmatched.contains(e.getName())) .collect(Collectors.toSet()));
			
	
		} catch (Exception e) {
			dedicatedLogger.error("Unexpected error",e);
		}
	

	}



}

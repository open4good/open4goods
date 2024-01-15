//package org.open4goods.api.services.aggregation.services.batch;
//
//import java.util.HashSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
// TODO : Merge with AttributeAggregationService

//import org.open4goods.api.services.aggregation.AbstractAggregationService;

//import org.open4goods.config.yml.ui.AttributesConfig;
//import org.open4goods.config.yml.ui.VerticalConfig;
//import org.open4goods.model.product.Product;
//import org.open4goods.services.VerticalsConfigService;
//
///**
// * Service in charge of mapping product categories to verticals
// * @author goulven
// *
// */
//public class UnmappedAttributeCleaningBatchAggregationService extends AbstractAggregationService {
//
//	private VerticalsConfigService verticalConfigService;
//
//	public UnmappedAttributeCleaningBatchAggregationService( final String logsFolder,  VerticalsConfigService verticalConfigService, boolean toConsole) {
//		super(logsFolder, toConsole);
//		this.verticalConfigService = verticalConfigService;
//		
//	}
//
//	@Override
//	public void onProduct(Product product, VerticalConfig vConf) {
//
//
//		
//		try {
//			
//			AttributesConfig attributesConfig = vConf.getAttributesConfig() ;
//
//			// Adding the list of "to be removed" attributes
//			Set<String> toRemoveFromUnmatched = new HashSet<>(attributesConfig.getExclusions());
//		
//			// Removing 
//			product.getAttributes().setUnmapedAttributes(product.getAttributes().getUnmapedAttributes().stream().filter(e -> !toRemoveFromUnmatched.contains(e.getName())) .collect(Collectors.toSet()));
//			
//	
//		} catch (Exception e) {
//			dedicatedLogger.error("Unexpected error",e);
//		}
//	
//
//	}
//
//
//
//}

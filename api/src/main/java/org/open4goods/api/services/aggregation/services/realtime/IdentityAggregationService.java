
package org.open4goods.api.services.aggregation.services.realtime;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is in charge of interprting barcode countries and of generating
 * barcode images
 *
 * @author Goulven.Furet
 *
 */
public class IdentityAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(IdentityAggregationService.class);

	private Gs1PrefixService gs1Service;

	private BarcodeValidationService validationService;

	public IdentityAggregationService(final Logger logger, final Gs1PrefixService gs1Service, final BarcodeValidationService barcodeValidationService) {
		super(logger);
		this.gs1Service = gs1Service;
		validationService = barcodeValidationService;

	}

	@Override
	public  Map<String, Object> onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {

		/////////////////////////////
		// Validating barcodes
		/////////////////////////////
		
		if (null == output.getId()) {
			// TODO(p2, features) : Should store the GTIN type when encountered in gtin infos, and then render with appropriate leading 0
			output.setId(Long.valueOf(input.gtin()));
		} else {
			if (!output.gtin().equals(input.gtin())) {
				dedicatedLogger.error("GTIN Mismatch : product {], dataFragment {}", output.gtin(), input.gtin());
			}
		}
		
		/////////////////////////////
		// Adding alternate models's
		/////////////////////////////		
		output.getAkaModels().addAll(input.getAlternateIds());

		/////////////////////////////
		// Setting the dates
		/////////////////////////////
		
		// The last update
		output.setLastChange(System.currentTimeMillis());
		
		onProduct(output, vConf);
		return null;
	}

	@Override
	public void onProduct(Product output, VerticalConfig vConf) throws AggregationSkipException {

		// TODO
		// Erase ai review if old format
		
		
		AiReview review = output.getAiReviews().get("fr");
		if (review != null && review.attributes() == null )  {
			// A old one
			output.getAiReviews().clear();
			logger.info("Cleared review for : {}" , output );
		}
		
		if (review != null && review.attributes() != null )  {
			// A actual one
			logger.info("Up to date review for : {}" , output );

		}
		
		
		if (StringUtils.isEmpty(output.gtin())) {
			dedicatedLogger.warn("Skipping product aggregation, empty barcode");
			throw new AggregationSkipException("Cannot proceed, empty barcode");
		} 

		SimpleEntry<BarcodeType, String> valResult = validationService.sanitize(output.gtin());

		if (valResult.getKey().equals(BarcodeType.UNKNOWN)) {
			dedicatedLogger.info("{} is not a valid ISBN/UEAN13 barcode : {}",valResult.getValue() ,output.gtin());
			throw new AggregationSkipException("Invalid barcode : " + output.gtin());
		}

		// Replacing the barcode, due to sanitisation
		output.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN,valResult.getValue());
		output.setId(Long.valueOf(valResult.getValue()));


	
			
		/////////////////////////////
		// Adding country information
		/////////////////////////////

		if (valResult.getKey().equals(BarcodeType.GTIN_13) || valResult.getKey().equals(BarcodeType.GTIN_12) || valResult.getKey().equals(BarcodeType.GTIN_14) || valResult.getKey().equals(BarcodeType.GTIN_8)) {

			String country = gs1Service.detectCountry(output.gtin());
			//		logger.info("Country for {} is {}", output.gtin(), country);
			output.getGtinInfos().setCountry(country);
		} else if(valResult.getKey().equals(BarcodeType.ISBN_13)) {
			dedicatedLogger.info("A GTIN type, cannot complete with country and type : {}",output.getId());
		} else {
			dedicatedLogger.warn("An unknown GTIN/ISBN type, cannot complete with country and type : {}",output.getId());
		}

		// Setting barcode type
		output.getGtinInfos().setUpcType(valResult.getKey());
		
	}

}

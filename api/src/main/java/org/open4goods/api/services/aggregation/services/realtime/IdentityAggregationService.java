
package org.open4goods.api.services.aggregation.services.realtime;

import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.BarcodeType;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.Gs1PrefixService;
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
	public void onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {

		/////////////////////////////
		// Validating barcodes
		/////////////////////////////
		
		if (null == output.gtin()) {
			output.setId(input.gtin());
		} else {
			if (!output.gtin().equals(input.gtin())) {
				dedicatedLogger.error("GTIN Mismatch : product {], dataFragment {}", output.gtin(), input.gtin());
			}
		}
		
		/////////////////////////////
		// Adding alternate id's
		/////////////////////////////		
		output.getAlternativeIds().addAll(input.getAlternateIds());

		/////////////////////////////
		// Setting the dates
		/////////////////////////////
		
		// The last update
		if ( output.getLastChange() < input.getLastIndexationDate()) {
			output.setLastChange(input.getLastIndexationDate());
		}

		/////////////////////////////
		// Updating the datasources
		/////////////////////////////
		output.getDatasourceNames().add(input.getDatasourceName());

		
		onProduct(output, vConf);
	}

	@Override
	public void onProduct(Product output, VerticalConfig vConf) throws AggregationSkipException {

		if (StringUtils.isEmpty(output.gtin())) {
			dedicatedLogger.warn("Skipping product aggregation, empty barcode");
			throw new AggregationSkipException("Cannot proceed, empty barcode");
		} 

		SimpleEntry<BarcodeType, String> valResult = validationService.sanitize(output.gtin());

		if (valResult.getKey().equals(BarcodeType.UNKNOWN)) {
			dedicatedLogger.warn("{} is not a valid ISBN/UEAN13 barcode : {}",valResult.getValue() ,output.gtin());
			throw new AggregationSkipException("Invalid barcode : " + output.gtin());
		}

		// Replacing the barcode, due to sanitisation
		output.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN,valResult.getValue());
		output.setId(valResult.getValue());


		/////////////////////////////
		// Adding country information
		/////////////////////////////

		if (valResult.getKey().equals(BarcodeType.GTIN_13) || valResult.getKey().equals(BarcodeType.GTIN_12) || valResult.getKey().equals(BarcodeType.GTIN_14) || valResult.getKey().equals(BarcodeType.GTIN_8)) {

			String country = gs1Service.detectCountry(output.gtin());
			//		logger.info("Country for {} is {}", output.gtin(), country);
			output.getGtinInfos().setCountry(country);
		}

		// Setting barcode type
		output.getGtinInfos().setUpcType(valResult.getKey());
		
	}

}


package org.open4goods.aggregation.services.aggregation;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
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
public class BarCodeAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(BarCodeAggregationService.class);

	private Gs1PrefixService gs1Service;

	public BarCodeAggregationService(final String logsFolder, final Gs1PrefixService gs1Service) {
		super(logsFolder);
		this.gs1Service = gs1Service;

	}

	@Override
	public void onDataFragment(final DataFragment input, final AggregatedData output) {

		/////////////////////////////
		// Adding country information
		/////////////////////////////

		String country = gs1Service.detectCountry(output.gtin());
//		logger.info("Country for {} is {}", output.gtin(), country);
		output.getGtinInfos().setCountry(country);

	}

}

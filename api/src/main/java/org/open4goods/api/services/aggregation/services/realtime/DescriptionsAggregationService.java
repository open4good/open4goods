package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Description;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
public class DescriptionsAggregationService extends AbstractAggregationService{

	public DescriptionsAggregationService(Logger logger) {
		super(logger);

	}

	@Override
	public void onDataFragment(DataFragment o, final Product output, VerticalConfig vConf) throws AggregationSkipException {
		// Handle descriptions for a datafragment
		handleDescriptions(o.getDescriptions(), output, vConf);
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
		// Handle descriptions for a product
		handleDescriptions(data.getDescriptions(), data,vConf);
	}

	/**
	 * Handle descriptions, including cleaning and truncation
	 * @param descriptions
	 * @param output
	 */
	private void handleDescriptions( final Set<Description> descriptions, Product output, VerticalConfig vConf) {
		for (final Description d : descriptions) {

			// Strip html tags
			String text = Jsoup.parse(d.getContent().getText()).text();

			// Normalizing spaces
			text = StringUtils.normalizeSpace(text);

			// Cut text

			// NOTE(gof) : Not really nice, should clone. But i know that the descriptions
			// are not used after the "computeDescriptions", that's my archi !
			if (text.length() > vConf.getDescriptionsAggregationConfig().getDescriptionsTruncationLength()) {
				text = text.substring(0, vConf.getDescriptionsAggregationConfig().getDescriptionsTruncationLength()) + vConf.getDescriptionsAggregationConfig().getDescriptionsTruncationSuffix();
				d.setTruncated(true);
			} else {
				d.setTruncated(false);
			}

			d.getContent().setText(text);
			//				d.setProvider
			output.getDescriptions().add(d);
		}
	}

	

}

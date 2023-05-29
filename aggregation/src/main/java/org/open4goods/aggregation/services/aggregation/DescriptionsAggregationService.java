package org.open4goods.aggregation.services.aggregation;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.DescriptionsAggregationConfig;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Description;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DescriptionsAggregationService extends AbstractAggregationService{

	private static final Logger logger = LoggerFactory.getLogger(DescriptionsAggregationService.class);

	private final DescriptionsAggregationConfig config;

	public DescriptionsAggregationService(final DescriptionsAggregationConfig config,final String logsFolder) {
		super(logsFolder);
		this.config = config;
	}

	@Override
	public void onDataFragment(DataFragment o, final Product output) {


		for (final Description d : o.getDescriptions()) {

			// Strip html tags
			String text = Jsoup.parse(d.getContent().getText()).text();

			// Normalizing spaces
			text = StringUtils.normalizeSpace(text);

			// Cut text

			// NOTE(gof) : Not really nice, should clone. But i know that the descriptions
			// are not used after the "computeDescriptions", that's my archi !
			if (text.length() > config.getDescriptionsTruncationLength()) {
				text = text.substring(0, config.getDescriptionsTruncationLength()) + config.getDescriptionsTruncationSuffix();
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

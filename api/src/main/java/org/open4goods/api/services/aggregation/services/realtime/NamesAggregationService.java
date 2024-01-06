package org.open4goods.api.services.aggregation.services.realtime;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.attributes.NamesConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamesAggregationService extends AbstractRealTimeAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(NamesAggregationService.class);



	private EvaluationService evaluationService;

	private VerticalsConfigService verticalService;


	public NamesAggregationService(final String logsFolder,boolean toConsole, final VerticalsConfigService verticalService, EvaluationService evaluationService) {
		super(logsFolder,toConsole);
		this.evaluationService = evaluationService;
		this.verticalService = verticalService;
	}

	@Override
	public void onDataFragment(final DataFragment df, Product output) {

		// Adding raw offer names
		// TODO : names are not localized
		output.getNames().getOfferNames().addAll(df.getNames().stream()
				.map(this::normalizeName).collect(Collectors.toSet()));
		
		output = productHandling(output);
		
	}
	
	// TODO : Generaliser pour permettre traitements de reprise batch
	public Product productHandling(Product data) {
		
		logger.info("Name generation for product {}", data.getId());
		
		// Getting the config for the category, if any
		VerticalConfig vConf = verticalService.getConfigByIdOrDefault(data.getVertical());
		
		if (null == vConf) {
			vConf = verticalService.getDefaultConfig();
		}
	
		
		if (null != vConf ){
		
			////////////////
			// AI generation		
			/////////////////
			List<NamesConfig> namesConfigs = vConf.getNamesConfig();
			for (NamesConfig nameConfig : namesConfigs) {				
				String key = nameConfig.getKey();
								
				for (Entry<String, String> i18n : nameConfig.getValue().entrySet()) {
					String lang = i18n.getKey();
					String value = evaluationService.thymeleafEval(data, i18n.getValue());
					data.getNames().addName(lang, key, value);
				}				
			}
		}
		return data;
	}

	
	private String normalizeName( String name) {
		return name.trim();
	}

}

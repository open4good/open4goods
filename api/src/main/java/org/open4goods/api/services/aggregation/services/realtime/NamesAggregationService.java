package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.ui.TextsConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.textgen.BlablaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamesAggregationService extends AbstractRealTimeAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(NamesAggregationService.class);

	private EvaluationService evaluationService;

	private VerticalsConfigService verticalService;

	private BlablaService blablaService;
	
	public NamesAggregationService(final String logsFolder, boolean toConsole,
			final VerticalsConfigService verticalService, EvaluationService evaluationService, BlablaService blablaService) {
		super(logsFolder, toConsole);
		this.evaluationService = evaluationService;
		this.verticalService = verticalService;
		this.blablaService = blablaService;
	}

	@Override
	public void onDataFragment(final DataFragment df, Product output) throws AggregationSkipException {

		// Adding raw offer names
		// TODO : names are not localized
		output.getNames().getOfferNames()
				.addAll(df.getNames().stream().map(this::normalizeName).collect(Collectors.toSet()));

		handle(output);

	}



	@Override
	public void handle(Product data) throws AggregationSkipException {
		logger.info("Name generation for product {}", data.getId());

		// Getting the config for the category, if any
		Map<String, TextsConfig> tConfs = verticalService.getConfigByIdOrDefault(data.getVertical()).getTexts();

		// For each language
		for (Entry<String, TextsConfig> e : tConfs.entrySet()) {

			try {
				String lang = e.getKey();
				TextsConfig tConf = e.getValue();
				
				///////////////////////
				// Computing url
				//////////////////////
				String url = computeUrl(data, tConf.getUrl());
				data.getNames().getUrl().put(lang, url);
				
				
				
				
			} catch (InvalidParameterException e1) {
				logger.error("Error while computing url for product {}", data.getId(), e1);
			}

			
			
			
		}
		
		
		
		
//		if (null != vConf) {
//
//			////////////////
//			// AI generation
//			/////////////////
//			List<NamesConfig> namesConfigs = vConf.getNamesConfig();
//			for (NamesConfig nameConfig : namesConfigs) {
//				String key = nameConfig.getKey();
//
//				for (Entry<String, String> i18n : nameConfig.getValue().entrySet()) {
//					String lang = i18n.getKey();
//					String value = evaluationService.thymeleafEval(data, i18n.getValue());
//					data.getNames().addName(lang, key, value);
//				}
//			}
//		}
	}

	
	/**
	 * 
	 * @param data
	 * @param url
	 * @return
	 * @throws InvalidParameterException 
	 */
	private String computeUrl(Product data, String url) throws InvalidParameterException {
		
		
		return blablaService.generateBlabla(url, data);
	}

	private String normalizeName(String name) {
		return name.trim();
	}
	
}

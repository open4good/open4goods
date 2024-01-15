package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.ui.TextsConfig;
import org.open4goods.config.yml.ui.PrefixedAttrText;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedAttribute;
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
				
				// Computing url

				data.getNames().getUrl().put(lang, computePrefixedText(data, tConf.getUrl()));
				
				// h1Title			
				data.getNames().getH1Title().put(lang, computePrefixedText(data, tConf.getH1Title()));
				
				// metaTitle
				data.getNames().getMetaTitle().put(lang, blablaService.generateBlabla(tConf.getMetaTitle(), data));
				
				// metaDescription
				data.getNames().getMetaDescription().put(lang, blablaService.generateBlabla(tConf.getMetaDescription(), data));
				
				// opengraphTitle
				data.getNames().getOpengraphTitle().put(lang, blablaService.generateBlabla(tConf.getOpengraphTitle(), data));
				
				// openGraphDescription
				data.getNames().getOpenGraphDescription().put(lang, blablaService.generateBlabla(tConf.getOpenGraphDescription(), data));
				
				// twitterTitle
				data.getNames().getTwitterTitle().put(lang, blablaService.generateBlabla(tConf.getTwitterTitle(), data));
				
				// twitterDescription
				data.getNames().getTwitterDescription().put(lang, blablaService.generateBlabla(tConf.getTwitterDescription(), data));
				

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
	 * @param textsConfigUrl
	 * @return
	 * @throws InvalidParameterException 
	 */
	private String computePrefixedText(Product data, PrefixedAttrText textsConfigUrl) throws InvalidParameterException {
		
		StringBuilder sb = new StringBuilder(data.gtin());
		
		// Adding the prefix
		String prefix = blablaService.generateBlabla(textsConfigUrl.getPrefix(), data);
		if (!StringUtils.isEmpty(prefix)) {			
			sb.append("-").append(prefix);
		}
		
		// Adding the mentioned attrs if existing		
		for (String attr : textsConfigUrl.getAttrs()) {
			// Checking in referentiel attrs
			String refVal = null;
			if (ReferentielKey.isValid(attr)) {
				refVal = data.getAttributes().getReferentielAttributes().get(ReferentielKey.valueOf(attr));
			}
			
			if (null != refVal) {
				sb.append("-").append(IdHelper.azCharAndDigits(refVal).toLowerCase());
			} else {
				// Checking in aggregated attrs
				AggregatedAttribute attrValue = data.getAttributes().getAggregatedAttributes().get(attr);
				if (null != attrValue) {
					sb.append("-").append(attrValue.getValue().toLowerCase());
				} 
			}
		}
		
		return sb.toString();
	}

	private String normalizeName(String name) {
		return name.trim();
	}
	
}

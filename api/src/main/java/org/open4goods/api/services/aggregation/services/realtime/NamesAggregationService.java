package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.PrefixedAttrText;
import org.open4goods.config.yml.ui.I18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
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

public class NamesAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(NamesAggregationService.class);

	private EvaluationService evaluationService;

	private VerticalsConfigService verticalService;

	private BlablaService blablaService;
	
	public NamesAggregationService(final Logger logger,
			final VerticalsConfigService verticalService, EvaluationService evaluationService, BlablaService blablaService) {
		super(logger);
		this.evaluationService = evaluationService;
		this.verticalService = verticalService;
		this.blablaService = blablaService;
	}

	@Override
	public void onDataFragment(final DataFragment df, Product output, VerticalConfig vConf) throws AggregationSkipException {

		// Adding raw offer names
		// TODO : names are not localized
		output.getNames().getOfferNames()
				.addAll(df.getNames().stream().map(this::normalizeName).collect(Collectors.toSet()));

		onProduct(output,vConf);

	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		logger.info("Name generation for product {}", data.getId());

		// Getting the config for the category, if any
		Map<String, I18nElements> tConfs = verticalService.getConfigByIdOrDefault(data.getVertical()).getI18n();

		// For each language
		for (Entry<String, I18nElements> e : tConfs.entrySet()) {

			try {
				String lang = e.getKey();
				I18nElements tConf = e.getValue();
				
				// Computing url
				data.getNames().getUrl().put(lang, data.gtin() + "-" + computePrefixedText(data, tConf.getUrl(), "-"));
				// h1Title			
				data.getNames().getH1Title().put(lang, computePrefixedText(data, tConf.getH1Title(), " "));
				// metaTitle
				data.getNames().getMetaTitle().put(lang, blablaService.generateBlabla(tConf.getProductMetaTitle(), data));
				// metaDescription
				data.getNames().getMetaDescription().put(lang, blablaService.generateBlabla(tConf.getProductMetaDescription(), data));
				// productMetaOpenGraphTitle
				data.getNames().getProductMetaOpenGraphTitle().put(lang, blablaService.generateBlabla(tConf.getProductMetaOpenGraphTitle(), data));
				// productMetaOpenGraphDescription
				data.getNames().getproductMetaOpenGraphDescription().put(lang, blablaService.generateBlabla(tConf.getProductMetaOpenGraphDescription(), data));
				// productMetaTwitterTitle
				data.getNames().getproductMetaTwitterTitle().put(lang, blablaService.generateBlabla(tConf.getProductMetaTwitterTitle(), data));
				// productMetaTwitterDescription
				data.getNames().getproductMetaTwitterDescription().put(lang, blablaService.generateBlabla(tConf.getProductMetaTwitterDescription(), data));

			
			
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
	private String computePrefixedText(Product data, PrefixedAttrText textsConfigUrl, String separator) throws InvalidParameterException {
		
		StringBuilder sb = new StringBuilder();
		
		// Adding the prefix
		String p = textsConfigUrl.getPrefix();
		
		if (!StringUtils.isEmpty(p)) {
			String prefix = blablaService.generateBlabla(p, data);
			if (!StringUtils.isEmpty(prefix)) {			
				sb.append(prefix);
			}
		}
		
		// Adding the mentioned attrs if existing		
		for (String attr : textsConfigUrl.getAttrs()) {
			// Checking in referentiel attrs
			String refVal = null;
			if (ReferentielKey.isValid(attr)) {
				refVal = data.getAttributes().getReferentielAttributes().get(ReferentielKey.valueOf(attr));
			}
			
			if (null != refVal) {
				sb.append(separator).append(IdHelper.azCharAndDigits(refVal).toLowerCase());
			} else {
				// Checking in aggregated attrs
				AggregatedAttribute attrValue = data.getAttributes().getAggregatedAttributes().get(attr);
				if (null != attrValue) {
					sb.append(separator).append(attrValue.getValue().toLowerCase());
				} 
			}
		}
		
		return sb.toString();
	}

	private String normalizeName(String name) {
		return name.trim();
	}

	
}

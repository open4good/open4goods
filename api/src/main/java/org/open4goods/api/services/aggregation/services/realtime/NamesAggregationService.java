package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.evaluation.service.EvaluationService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
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
	public  Map<String, Object> onDataFragment(final DataFragment df, Product output, VerticalConfig vConf) throws AggregationSkipException {

		// Adding raw offer names
		// TODO : names are not localized
		output.getOfferNames()
				.addAll(df.getNames().stream().map(this::normalizeName).collect(Collectors.toSet()));

		onProduct(output,vConf);
		return null;

	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		logger.info("Name generation for product {}", data.getId());

		// Getting the config for the category, if any
		Map<String, ProductI18nElements> tConfs = verticalService.getConfigByIdOrDefault(data.getVertical()).getI18n();

		// For each language
		for (Entry<String, ProductI18nElements> e : tConfs.entrySet()) {

			try {
				String lang = e.getKey();
				ProductI18nElements tConf = e.getValue();
				
				// Computing url
				if (vConf.isForceNameGeneration() ||  (  null == data.getNames().getUrl().get(lang))) {
					logger.info("Generating  product url for {}", data);
					PrefixedAttrText urlPrefix = tConf.getUrl();
					String url = generateUrl(data, urlPrefix, vConf);
										
					data.getNames().getUrl().put(lang, url);
				} else {
					logger.info("Skipping URL generation for {}", data);
				}
				
				if (vConf.isForceNameGeneration() || data.getVertical() == null || null == data.getNames().getH1Title().get(lang)) {
					// h1Title			
					data.getNames().getH1Title().put(lang, computePrefixedText(data, tConf.getH1Title(), " "));
				}
				
				// metaDescription
				data.getNames().getMetaDescription().put(lang, blablaService.generateBlabla(tConf.getProductMetaDescription(), data));
				// productMetaOpenGraphTitle
				data.getNames().getProductMetaOpenGraphTitle().put(lang, blablaService.generateBlabla(tConf.getProductMetaOpenGraphTitle(), data));
				// productMetaOpenGraphDescription
				data.getNames().getProductMetaOpenGraphDescription().put(lang, blablaService.generateBlabla(tConf.getProductMetaOpenGraphDescription(), data));
				// productMetaTwitterTitle
				data.getNames().getProductMetaTwitterTitle().put(lang, blablaService.generateBlabla(tConf.getProductMetaTwitterTitle(), data));
				// productMetaTwitterDescription
				data.getNames().getProductMetaTwitterDescription().put(lang, blablaService.generateBlabla(tConf.getProductMetaTwitterDescription(), data));

			
			
			} catch (InvalidParameterException e1) {
				logger.error("Error while computing url for product {}", data.getId(), e1);
			}
		}
	}

	private String generateUrl(Product data, PrefixedAttrText urlPrefix, VerticalConfig vConf) throws InvalidParameterException {
		
		String url = data.gtin();
		String urlSuffix;
		
		if (null == vConf.getId()) {
			// It is an undefined vertical, we apply shortest offername
			 urlSuffix = data.shortestOfferName();
		} else {
			// It is a defined vertical, we apply names 
			urlSuffix = StringUtils.stripAccents(computePrefixedText(data, urlPrefix, "-"));
		}

		if (!StringUtils.isEmpty(urlSuffix)) {
			url +=  "-" + urlSuffix;
		}

		// Url sanitisation
		url = StringUtils.normalizeSpace(url).replace(' ', '-');
		url = url.replaceAll("-{2,}", "-");
		url = url.toLowerCase();
		if (url.endsWith("-")) {
			url = url.substring(0,url.length()-1);
		}
		
		return url;
		
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
			String refVal = data.getAttributes().val(attr);
			
			if (null != refVal) {
				sb.append(separator).append(IdHelper.azCharAndDigits(refVal).toLowerCase());
			} 
		}
		
		return sb.toString();
	}

	private String normalizeName(String name) {
		return name.trim();
	}

	
}

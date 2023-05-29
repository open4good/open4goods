//package org.open4goods.aggregation.services.aggregation;
//
//import java.util.Map;
//import java.util.Map.Entry;
//
//import org.open4goods.aggregation.AbstractAggregationService;
//import org.open4goods.model.Localisable;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.product.Product;
//import org.open4goods.model.product.Urls;
//import org.open4goods.services.EvaluationService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class UrlsAggregationService extends AbstractAggregationService {
//
//	private static final Logger logger = LoggerFactory.getLogger(UrlsAggregationService.class);
//
//
//
//	private final EvaluationService evaluationService;
//
//	private final Localisable productTemplates;
//
//	public UrlsAggregationService( final EvaluationService evaluationService,final String logsFolder, Localisable productTemplates) {
//		super(logsFolder);
//		this.evaluationService = evaluationService;
//		this.productTemplates = productTemplates;
//		
//	}
//
//	@Override
//	public void onDataFragment(final DataFragment input, final Product output) {
//		final Urls urls = new Urls();
//
//		for (final Entry<String, String> tpl : productTemplates.entrySet()) {
//			urls.getUrls().put(tpl.getKey(), getProductUrl(output, tpl.getKey()));
//		}
//		output.setUrls(urls);
//	}
//
//	public String getProductUrl(final Product p, final String language) {
////		String template = null;
////		try {
////				template = productTemplates.getOrDefault(language, productTemplates.get("default")).replace("!{", "${");
////
////				return URLEncoder.encode(evaluationService.thymeleafEval(p, template), Charset.defaultCharset().toString());
////		} catch (final Exception e) {
////			dedicatedLogger.error("Unable to generate urls for {} with template {}. Error is : {}", p, template, e.getMessage());
////		}
//		return p.getId();
//	}
//
//}

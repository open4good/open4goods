package org.open4goods.ui.helper;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.product.Product;

import jakarta.servlet.http.HttpServletRequest;

public class UiHelper {

	
	private HttpServletRequest request;
	private VerticalConfig verticalConfig;

	private Map<String,String> texts = new HashMap<>();
	private Product product;
	
	public static final DecimalFormat numberFormater = new DecimalFormat("0.#");

	
	public UiHelper(HttpServletRequest request, VerticalConfig verticalConfig, Product product) {
		super();
		this.request = request;
		this.verticalConfig = verticalConfig;
		this.product = product;
		
		// Maybe not the best way to inject texts
		texts.put("title", product.getNames().getH1Title().i18n(request));
		texts.put("meta-description", product.getNames().getMetaDescription().i18n(request));
		texts.put("twitter-description", product.getNames().getproductMetaTwitterDescription().i18n(request));
		texts.put("twitter-title", product.getNames().getproductMetaTwitterTitle().i18n(request));
		texts.put("opengraph-description", product.getNames().getproductMetaOpenGraphDescription().i18n(request));
		texts.put("opengraph-title", product.getNames().getProductMetaOpenGraphTitle().i18n(request));
		
	}
	
	/**
	 * TODO : i18n
	 * Programmatic because much so easy
	 * @return
	 */
	public String getMetaTitle() {
		StringBuilder sb = new StringBuilder();
		
			sb.append(product.bestName());

			if (product.ecoscore() != null) {
				sb.append(" > Eco-score : ");
				sb.append(numberFormater.format(product.ecoscore().getRelativ().getValue()));
				sb.append("/5");
			}
			
			if (null != product.bestPrice()) {
				sb.append(" - contribution écologique : ");
				sb.append(numberFormater.format(product.bestPrice().getCompensation()));
				sb.append("€");
			}
			return sb.toString();
	}
	
	/**
	 * Return the utl
	 * @return
	 */
	public String url() {
		return product.getNames().getUrl().i18n(request);
	}
	
	
	
	
	/**
	 * Return the i18n for an attribute
	 * @param key
	 * @return
	 */
	public String attributeName(String key) {
		
		if (null == verticalConfig) {
			return key;
		}
		
		 AttributeConfig attr = verticalConfig.getAttributesConfig().getAttributeConfigByKey(key);
		 
		 if (null == attr) {
			 return key +" (!)";
		 }
		return attr.i18n(request.getLocale().getLanguage());
		
		
	}




	public Map<String, String> getTexts() {
		return texts;
	}




	public void setTexts(Map<String, String> texts) {
		this.texts = texts;
	}





}

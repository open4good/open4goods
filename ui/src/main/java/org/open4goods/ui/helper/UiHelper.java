package org.open4goods.ui.helper;

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
	
	
	public UiHelper(HttpServletRequest request, VerticalConfig verticalConfig, Product product) {
		super();
		this.request = request;
		this.verticalConfig = verticalConfig;
		this.product = product;
		
		// Maybe not the best way to inject texts
		texts.put("title", product.getNames().getH1Title().i18n(request));
		texts.put("meta-description", product.getNames().getMetaDescription().i18n(request));
		texts.put("meta-title", product.getNames().getMetaTitle().i18n(request));
		texts.put("twitter-description", product.getNames().getTwitterDescription().i18n(request));
		texts.put("twitter-title", product.getNames().getTwitterTitle().i18n(request));
		texts.put("opengraph-description", product.getNames().getOpenGraphDescription().i18n(request));
		texts.put("opengraph-title", product.getNames().getOpengraphTitle().i18n(request));
		
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

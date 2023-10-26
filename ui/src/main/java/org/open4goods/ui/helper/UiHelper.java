package org.open4goods.ui.helper;

import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.services.VerticalsConfigService;

import jakarta.servlet.http.HttpServletRequest;

public class UiHelper {

	
	private HttpServletRequest request;
	private VerticalConfig verticalConfig;
	
	public UiHelper(HttpServletRequest request, VerticalConfig verticalConfig) {
		super();
		this.request = request;
		this.verticalConfig = verticalConfig;
	}
	

	
	
	/**
	 * Return the i18n for an attribute
	 * @param key
	 * @return
	 */
	public String attributeName(String key) {
		
		 AttributeConfig attr = verticalConfig.getAttributesConfig().getAttributeConfigByKey(key);
		 
		 if (null == attr) {
			 return key +" (!)";
		 }
		return attr.i18n(request.getLocale().getLanguage());
		
		
	}





}

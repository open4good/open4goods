package org.open4goods.ui.helper;

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
		
		return verticalConfig.getAttributesConfig().getAttributeConfigByKey(key).i18n(request.getLocale().getLanguage());
		
		
	}





}

package org.open4goods.commons.config.yml.ui;

import java.util.ArrayList;
import java.util.List;

public class WebConfig {

	public static final String PRODUCT_MONOPAGE_TEMPLATE = "pages/product";

	public static final String PRODUCT_MULTIPAGE_TEMPLATE = "product-multipage";

	/**
	 * The google analytics ID, for page tracking
	 */
	private String googleAnalyticsId = "UNDEFINED";

	/**
	 * The google webmaster tools meta content for validating site
	 */
	private String googleSiteVerification;

	/**
	 * If true, the thymleaf templates will be cached (LRU method)
	 */
	private Boolean templatesCaching = false;

	/**
	 * The size of a products search recordset
	 */
	private Integer productsSearchMaxPageSize = 50;


	/** i18n cache duration in seconds**/
	private Integer i18nFileCache = 10;

	/**
	 * If true, will have to be logged to navigate on the website
	 */
	private Boolean webAuthentication = true;

	/**
	 * If true, the CSS used will the one that is hot built from scss sources, using gulp. It allows to switch the inclusion in templates/inc/header-meta.html
	 */
	private Boolean useHotScss = false;
	
	
	
	
	/**
	 * The list of hosts allowed for CORS
	 */
	private List<String> corsAllowedHosts = new ArrayList<>();


	public String getGoogleAnalyticsId() {
		return googleAnalyticsId;
	}

	public void setGoogleAnalyticsId(String googleAnalyticsId) {
		this.googleAnalyticsId = googleAnalyticsId;
	}

	public String getGoogleSiteVerification() {
		return googleSiteVerification;
	}

	public void setGoogleSiteVerification(String googleSiteVerification) {
		this.googleSiteVerification = googleSiteVerification;
	}

	public Boolean getTemplatesCaching() {
		return templatesCaching;
	}

	public void setTemplatesCaching(Boolean templatesCaching) {
		this.templatesCaching = templatesCaching;
	}

	public Integer getProductsSearchMaxPageSize() {
		return productsSearchMaxPageSize;
	}

	public void setProductsSearchMaxPageSize(Integer productsSearchMaxPageSize) {
		this.productsSearchMaxPageSize = productsSearchMaxPageSize;
	}

	public Integer getI18nFileCache() {
		return i18nFileCache;
	}

	public void setI18nFileCache(Integer i18nFileCache) {
		this.i18nFileCache = i18nFileCache;
	}

	public Boolean getWebAuthentication() {
		return webAuthentication;
	}

	public void setWebAuthentication(Boolean webAuthentication) {
		this.webAuthentication = webAuthentication;
	}

	public List<String> getCorsAllowedHosts() {
		return corsAllowedHosts;
	}

	public void setCorsAllowedHosts(List<String> corsAllowedHosts) {
		this.corsAllowedHosts = corsAllowedHosts;
	}

	public Boolean getUseHotScss() {
		return useHotScss;
	}

	public void setUseHotScss(Boolean useHotScss) {
		this.useHotScss = useHotScss;
	}

}

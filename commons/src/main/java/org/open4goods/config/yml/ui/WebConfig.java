package org.open4goods.config.yml.ui;

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




}

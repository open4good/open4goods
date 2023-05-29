
package org.open4goods.config.yml.ui;

import java.util.Locale;

import org.open4goods.model.Localisable;

import jakarta.validation.constraints.NotBlank;

public class SiteNaming {

	/**
	 * The template for the product ID's
	 */
	@NotBlank
	private String productIdTemplate;

	/**
	 * The product names templates, by language. use "default" for default languages
	 */
	private Localisable productNameTemplates = new Localisable();

	/** The products url template. Think SEO ! **/

	private Localisable productUrlTemplates = new Localisable();

	/**
	 * The url for the search page
	 */
	private Localisable searchUrl = new Localisable();

	/**
	 * The site names, per languages. Use "default" for the international, non
	 * language specific version
	 */
	private Localisable serverNames = new Localisable();

	/**
	 * The root url for each site, according to serverNames
	 */
	private Localisable baseUrls = new Localisable();

	public String getSiteName(final Locale l) {
		return serverNames.getOrDefault(l.getLanguage(), serverNames.get("default"));
	}

	public Localisable getServerNames() {
		return serverNames;
	}

	public void setServerNames(final Localisable base) {
		serverNames = base;
	}

	public Localisable getBaseUrls() {
		return baseUrls;
	}

	public void setBaseUrls(final Localisable baseUrls) {
		this.baseUrls = baseUrls;
	}

	public Localisable getSearchUrl() {
		return searchUrl;
	}

	public void setSearchUrl(final Localisable searchUrl) {
		this.searchUrl = searchUrl;
	}

	public Localisable getProductUrlTemplates() {
		return productUrlTemplates;
	}

	public void setProductUrlTemplates(Localisable productTemplates) {
		productUrlTemplates = productTemplates;
	}

	public String getProductIdTemplate() {
		return productIdTemplate;
	}

	public void setProductIdTemplate(String productIdTemplate) {
		this.productIdTemplate = productIdTemplate;
	}

	public Localisable getProductNameTemplates() {
		return productNameTemplates;
	}

	public void setProductNameTemplates(Localisable productNameTemplates) {
		this.productNameTemplates = productNameTemplates;
	}

}
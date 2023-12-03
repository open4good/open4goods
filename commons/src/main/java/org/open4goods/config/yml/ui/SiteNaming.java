
package org.open4goods.config.yml.ui;

import java.util.Locale;

import org.open4goods.model.Localisable;

public class SiteNaming {


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

	
}
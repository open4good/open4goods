
package org.open4goods.config.yml.ui;

import java.util.Locale;

import org.open4goods.model.Localisable;

public class SiteNaming {


	/**
	 * The site names, per languages. Use "default" for the international, non
	 * language specific version
	 */
	private Localisable<String,String> serverNames = new Localisable<>();

	/**
	 * The root url for each site, according to serverNames
	 */
	private Localisable<String,String> baseUrls = new Localisable<>();

	public String getSiteName(final Locale l) {
		return serverNames.getOrDefault(l.getLanguage(), serverNames.get("default"));
	}

	public Localisable<String, String> getServerNames() {
		return serverNames;
	}

	public void setServerNames(Localisable<String, String> serverNames) {
		this.serverNames = serverNames;
	}

	public Localisable<String, String> getBaseUrls() {
		return baseUrls;
	}

	public void setBaseUrls(Localisable<String, String> baseUrls) {
		this.baseUrls = baseUrls;
	}

	

	
}
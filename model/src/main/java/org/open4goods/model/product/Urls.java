package org.open4goods.model.product;

import java.util.Locale;

import org.open4goods.model.Localisable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class Urls {
	/**
	 * i18n product urls. Use "default" for default international name
	 */
	@Field(index = true, store = false, type = FieldType.Object)
	private Localisable<String,String> urls = new Localisable<>();

	/**
	 * Shortcut method to get the given name for a locale
	 *
	 * @param l
	 * @return
	 */
	public String getUrl(final Locale l) {
		return urls.getOrDefault(l.getLanguage(), urls.get("default"));
	}

	public String getUrl(final String l) {
		return urls.getOrDefault(l, urls.get("default"));
	}

	public Localisable<String, String> getUrls() {
		return urls;
	}

	public void setUrls(Localisable<String, String> urls) {
		this.urls = urls;
	}




}

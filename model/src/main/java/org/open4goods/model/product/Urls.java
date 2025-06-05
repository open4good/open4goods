package org.open4goods.model.product;

import java.util.Locale;

import org.open4goods.model.Localisable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public record Urls(@Field(index = true, store = false, type = FieldType.Object)
                   Localisable<String, String> urls) {

        public Urls() {
                this(new Localisable<>());
        }

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




}

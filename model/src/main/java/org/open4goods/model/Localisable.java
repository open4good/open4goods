
package org.open4goods.model;

import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;



public class Localisable<K,V> extends HashMap<String, V> {


	private static final long serialVersionUID = 7154423192084742663L;

	public V i18n(final String language) {
		if (null == language) {
			return i18n("default");
		}
		return getOrDefault(language, get("default"));
	}

}

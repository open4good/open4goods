
package org.open4goods.model.vertical;

import java.util.Locale;

import org.open4goods.model.Localisable;

public record SiteNaming(Localisable<String, String> serverNames,
                         Localisable<String, String> baseUrls) {

        public SiteNaming() {
                this(new Localisable<>(), new Localisable<>());
        }


	/**
	 * The site names, per languages. Use "default" for the international, non
	 * language specific version
	 */
        /**
         * Shortcut method to get the given name for a locale
         *
         * @param l
         * @return
         */

        public String getSiteName(final Locale l) {
                return serverNames.getOrDefault(l.getLanguage(), serverNames.get("default"));
        }

        public Localisable<String, String> getServerNames() {
                return serverNames;
        }

        public Localisable<String, String> getBaseUrls() {
                return baseUrls;
        }

	

	
}
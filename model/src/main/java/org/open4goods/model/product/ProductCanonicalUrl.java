package org.open4goods.model.product;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Single canonical rule for the public path of a product page.
 *
 * <p>The path is {@code /<verticalHomeUrl>/<slug>} for a product attached to a
 * vertical, and {@code /<slug>} otherwise. The front-api exposes it as
 * {@code fullSlug} (the Nuxt product page 301-redirects every other path to it
 * and uses it as {@code rel=canonical}), and the ui sitemap generation writes
 * the same value. Both must call this class so the sitemap never lists a URL
 * that redirects.</p>
 *
 * <p>The rule only depends on the product, its vertical configuration and the
 * domain language, never on the request locale, so a canonical URL is stable
 * whatever the client sends as {@code Accept-Language}.</p>
 */
public final class ProductCanonicalUrl {

	/** Localisation key holding the international fallback value. */
	public static final String DEFAULT_LANGUAGE_KEY = "default";

	private ProductCanonicalUrl() {
	}

	/**
	 * Resolve the canonical path of a product page.
	 *
	 * @param product        product to address
	 * @param verticalConfig configuration of the product vertical, or {@code null}
	 *                       when the product has no known vertical
	 * @param domainLanguage domain language of the site
	 * @return path starting with a slash, or {@code null} when the product has no
	 *         slug for this language
	 */
	public static String fullSlug(Product product, VerticalConfig verticalConfig, DomainLanguage domainLanguage) {
		if (product == null) {
			return null;
		}
		String slug = slug(product, domainLanguage);
		if (!hasText(slug)) {
			return null;
		}
		String verticalHomeUrl = hasText(product.getVertical())
				? verticalHomeUrl(verticalConfig, domainLanguage)
				: null;
		return path(slug, verticalHomeUrl);
	}

	/**
	 * Resolve the localised product slug ({@code names.url}).
	 *
	 * @param product        product to address
	 * @param domainLanguage domain language of the site
	 * @return slug, or {@code null} when none is available
	 */
	public static String slug(Product product, DomainLanguage domainLanguage) {
		if (product == null || product.getNames() == null) {
			return null;
		}
		return localise(product.getNames().getUrl(), domainLanguage);
	}

	/**
	 * Resolve the localised vertical home URL.
	 *
	 * @param verticalConfig vertical configuration, may be {@code null}
	 * @param domainLanguage domain language of the site
	 * @return vertical home URL, or {@code null} when none is configured
	 */
	public static String verticalHomeUrl(VerticalConfig verticalConfig, DomainLanguage domainLanguage) {
		if (verticalConfig == null) {
			return null;
		}
		ProductI18nElements i18n = localise(verticalConfig.getI18n(), domainLanguage);
		if (i18n != null && hasText(i18n.getVerticalHomeUrl())) {
			return i18n.getVerticalHomeUrl();
		}
		Map<String, ProductI18nElements> values = verticalConfig.getI18n();
		if (values == null) {
			return null;
		}
		for (String key : languageKeys(domainLanguage)) {
			ProductI18nElements candidate = values.get(key);
			if (candidate != null && hasText(candidate.getVerticalHomeUrl())) {
				return candidate.getVerticalHomeUrl();
			}
		}
		return null;
	}

	/**
	 * Join a vertical home URL and a slug into a canonical path.
	 *
	 * @param slug            product slug
	 * @param verticalHomeUrl vertical home URL, or {@code null} for an
	 *                        uncategorised product
	 * @return path starting with a slash, or {@code null} when the slug is blank
	 */
	public static String path(String slug, String verticalHomeUrl) {
		if (!hasText(slug)) {
			return null;
		}
		String trimmedSlug = trimSlashes(slug);
		if (!hasText(verticalHomeUrl)) {
			return "/" + trimmedSlug;
		}
		return "/" + trimSlashes(verticalHomeUrl) + "/" + trimmedSlug;
	}

	/**
	 * Ordered language keys tried when localising a value: the domain language
	 * in its usual spellings, then {@value #DEFAULT_LANGUAGE_KEY}.
	 *
	 * @param domainLanguage domain language, may be {@code null}
	 * @return ordered, de-duplicated keys
	 */
	public static Set<String> languageKeys(DomainLanguage domainLanguage) {
		LinkedHashSet<String> keys = new LinkedHashSet<>();
		if (domainLanguage != null) {
			String iso = domainLanguage.name();
			String normalizedTag = domainLanguage.languageTag().replace('_', '-');
			keys.add(iso);
			keys.add(iso.toLowerCase(Locale.ROOT));
			keys.add(iso.toUpperCase(Locale.ROOT));
			keys.add(domainLanguage.languageTag());
			keys.add(normalizedTag);
			keys.add(normalizedTag.toLowerCase(Locale.ROOT));
			if (normalizedTag.contains("-")) {
				String base = normalizedTag.substring(0, normalizedTag.indexOf('-'));
				keys.add(base);
				keys.add(base.toLowerCase(Locale.ROOT));
			}
		}
		keys.add(DEFAULT_LANGUAGE_KEY);
		return keys;
	}

	/**
	 * Pick the value for the domain language, then the default value, then the
	 * first non-null value.
	 */
	private static <T> T localise(Map<String, T> values, DomainLanguage domainLanguage) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		for (String key : languageKeys(domainLanguage)) {
			T value = values.get(key);
			if (value != null) {
				return value;
			}
		}
		return values.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
	}

	private static String trimSlashes(String value) {
		String trimmed = value.trim();
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

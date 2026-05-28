package org.open4goods.model.product;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Resolves canonical, displayable product names from stable product identity
 * data. This class is the single guardrail against leaking unresolved templates
 * or vertical category labels as product names.
 */
public final class ProductNameResolver {

	private static final Pattern UNRESOLVED_TEMPLATE_PATTERN = Pattern.compile(
			".*(\\[\\(\\$\\{[^}]+}\\)\\]|\\$\\{[^}]+}|\\{[A-Z0-9_]+}).*");

	private ProductNameResolver() {
	}

	/**
	 * Resolve the canonical product name.
	 *
	 * @param product product to name
	 * @param generatedName optional generated product-specific name
	 * @return a safe product name, or an empty string when no safe value exists
	 */
	public static String resolve(Product product, String generatedName) {
		if (product == null) {
			return "";
		}

		String brandModel = normalize(join(product.brand(), product.model()));
		if (isSafeProductName(brandModel, product)) {
			return brandModel;
		}

		String generated = normalize(generatedName);
		if (isSafeProductName(generated, product)) {
			return generated;
		}

		String offer = normalize(product.shortestOfferName());
		if (isSafeProductName(offer, product)) {
			return offer;
		}

		return normalize(product.gtin());
	}

	/**
	 * Reject blank, unresolved-template and category-only values.
	 *
	 * @param candidate possible product name
	 * @param product product identity used to detect generic labels
	 * @return true when the value is safe to expose
	 */
	public static boolean isSafeProductName(String candidate, Product product) {
		String normalized = normalize(candidate);
		if (StringUtils.isBlank(normalized) || containsUnresolvedTemplate(normalized)) {
			return false;
		}

		if (product == null) {
			return true;
		}

		String brand = normalize(product.brand());
		String model = normalize(product.model());
		if (StringUtils.isNotBlank(brand) && containsIgnoreCase(normalized, brand)) {
			return true;
		}
		if (StringUtils.isNotBlank(model) && containsIgnoreCase(normalized, model)) {
			return true;
		}

		String gtin = normalize(product.gtin());
		if (StringUtils.isNotBlank(gtin) && containsIgnoreCase(normalized, gtin)) {
			return true;
		}

		Set<String> offerNames = product.getOfferNames();
		if (offerNames != null && offerNames.stream().anyMatch(offer -> normalized.equals(normalize(offer)))) {
			return true;
		}

		return false;
	}

	/**
	 * Detect raw template syntax that must never be persisted or exposed.
	 *
	 * @param value candidate text
	 * @return true when template syntax remains unresolved
	 */
	public static boolean containsUnresolvedTemplate(String value) {
		return StringUtils.isNotBlank(value) && UNRESOLVED_TEMPLATE_PATTERN.matcher(value).matches();
	}

	private static boolean containsIgnoreCase(String value, String token) {
		return value.toLowerCase().contains(token.toLowerCase());
	}

	private static String normalize(String value) {
		return value == null ? "" : StringUtils.normalizeSpace(value.trim());
	}

	private static String join(String first, String second) {
		return StringUtils.normalizeSpace((StringUtils.defaultString(first) + " " + StringUtils.defaultString(second)).trim());
	}
}

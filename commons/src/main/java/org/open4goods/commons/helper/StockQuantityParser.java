package org.open4goods.commons.helper;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * Parses merchant-provided stock quantities into integer counts.
 */
public class StockQuantityParser {

	private static final Logger logger = GenericFileLogger.initLogger("product-stock-quantity-parser", Level.INFO, "/opt/open4goods/logs/");
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

	/**
	 * Parses a raw quantity value.
	 *
	 * @param val raw merchant value
	 * @return parsed stock quantity
	 * @throws InvalidParameterException when the value is empty or unknown
	 */
	public static Integer parse(final String val) throws InvalidParameterException {
		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate empty StockQuantity value");
		}

		String normalized = normalize(val);
		if (normalized.matches("\\d+\\+?")) {
			return Integer.valueOf(normalized.replace("+", ""));
		}

		Matcher matcher = NUMBER_PATTERN.matcher(normalized);
		if (matcher.find()) {
			return Integer.valueOf(matcher.group());
		}

		if (isKnownNonQuantityValue(normalized)) {
			logger.debug("Ignoring non-quantity StockQuantity value: raw='{}', normalized='{}'", val, normalized);
		} else {
			logger.warn("Unknown StockQuantity value: raw='{}', normalized='{}'", val, normalized);
		}
		throw new InvalidParameterException("Cannot parse StockQuantity value : " + val);
	}

	private static String normalize(String val) {
		String normalized = Normalizer.normalize(val.trim(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase()
				.replaceAll("[()_,;:/]", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return StringUtils.normalizeSpace(normalized);
	}

	private static boolean isKnownNonQuantityValue(String normalized) {
		return switch (normalized) {
		case "en stock", "instock", "in stock", "available", "disponible", "oui", "yes", "true",
				"out of stock", "outofstock", "rupture", "non", "no", "false", "unknown", "n a", "na", "nc" -> true;
		default -> false;
		};
	}

	private StockQuantityParser() {
	}
}

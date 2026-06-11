package org.open4goods.commons.helper;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * Parses merchant-provided shipping delays into a conservative number of days.
 */
public class ShippingTimeParser {

	private static final Logger logger = GenericFileLogger.initLogger("product-shipping-time-parser", Level.INFO, "/opt/open4goods/logs/");
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
	private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)\\s*(?:a|to|-)\\s*(\\d+)");
	// Pre-compiled to avoid Pattern.compile() on every normalize() call
	private static final Pattern P_DIACRITICS = Pattern.compile("\\p{M}");
	private static final Pattern P_PUNCTUATION = Pattern.compile("[()_,;:/]");
	private static final Pattern P_WHITESPACE  = Pattern.compile("\\s+");

	/**
	 * Parses a raw shipping time.
	 *
	 * @param val raw merchant value
	 * @return parsed delay in days
	 * @throws InvalidParameterException when the value is empty or unknown
	 */
	public static Integer parse(final String val) throws InvalidParameterException {
		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate empty ShippingTime value");
		}

		String tmp = normalize(val);
		switch (tmp) {
		case "livraison sous 3 a 5 jours":
			return 5;
		case "livraison sous 6 a 10 jours":
			return 10;
		case "sous 3 a 8 jours ouvres":
			return 8;
		case "demain":
		case "tomorrow":
		case "sous 1 jour ouvre":
			return 1;
		case "livraison sous 72h":
			return 3;
		case "aujourd hui":
		case "today":
		case "immediat":
		case "immediate":
		case "24h":
			return 1;
		default:
			break;
		}

		if (NumberUtils.isCreatable(tmp)) {
			return NumberUtils.createInteger(tmp);
		}

		Matcher range = RANGE_PATTERN.matcher(tmp);
		if (range.find()) {
			return Integer.valueOf(range.group(2));
		}

		Matcher number = NUMBER_PATTERN.matcher(tmp);
		if (number.find()) {
			int value = Integer.parseInt(number.group());
			if (tmp.contains("h") || tmp.contains("heure") || tmp.contains("hour")) {
				return Math.max(1, (int) Math.ceil(value / 24.0));
			}
			if (tmp.contains("semaine") || tmp.contains("week")) {
				return value * 7;
			}
			if (tmp.contains("mois") || tmp.contains("month")) {
				return value * 30;
			}
			if (tmp.contains("jour") || tmp.contains("day")) {
				return value;
			}
		}

		logger.debug("Unknown ShippingTime value: raw='{}', normalized='{}'", val, tmp);
		return null;
	}

	private static String normalize(String val) {
		String normalized = P_DIACRITICS.matcher(Normalizer.normalize(val.trim(), Normalizer.Form.NFD)).replaceAll("")
				.toLowerCase(Locale.ROOT);
		normalized = P_PUNCTUATION.matcher(normalized).replaceAll(" ");
		normalized = P_WHITESPACE.matcher(normalized).replaceAll(" ").trim();
		return normalized;
	}
}

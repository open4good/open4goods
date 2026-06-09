package org.open4goods.commons.helper;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * Parses merchant-provided shipping costs into an amount in the offer currency.
 *
 * <p>Supports several raw value formats found in affiliate feeds:
 * <ul>
 *   <li>Plain numeric: {@code "4.99"}, {@code "0"}</li>
 *   <li>With currency symbol: {@code "4.99 EUR"}, {@code "$0"}</li>
 *   <li>Awin country-prefixed: {@code "FR:::4.99 EUR"}, {@code "FR:::0 EUR"}</li>
 *   <li>Free-shipping tokens: {@code "gratuit"}, {@code "offerte"}, {@code "free shipping"}</li>
 * </ul>
 */
public class ShippingCostParser {

	// TODO : Path from conf
	private static final Logger logger = GenericFileLogger.initLogger("product-shipping-cost-parser", Level.INFO, "/opt/open4goods/logs/");

	/** Matches Awin-style {@code COUNTRY:::} prefix, e.g. {@code "FR:::"}, {@code "DE:::"}. */
	private static final Pattern COUNTRY_PREFIX = Pattern.compile("^[A-Za-z]{2,3}:::");

	/** Currency symbols to strip. */
	private static final Pattern CURRENCY_SYMBOLS = Pattern.compile("[€$£]");

	/** Whole-word currency codes / tax tokens to strip. */
	private static final Pattern CURRENCY_WORDS = Pattern.compile("\\b(eur|euro|euros|usd|gbp|ttc|ht)\\b");

	/** Characters to replace with a space (keeps digits, dot, sign, and lower-alpha). */
	private static final Pattern KEEP_DECIMAL = Pattern.compile("[^0-9.+\\-a-z ]");

	/**
	 * Parses a raw shipping cost.
	 *
	 * @param val raw merchant value
	 * @return parsed cost as a {@code Double}
	 * @throws InvalidParameterException when the value is empty or unrecognised
	 */
	public static Double parse(final String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate empty ShippingCost value");
		}

		String normalized = normalize(val);
		if (isFreeShipping(normalized)) {
			return 0.0;
		}

		if (NumberUtils.isCreatable(normalized)) {
			return NumberUtils.createDouble(normalized);
		}

		logger.warn("Unknown ShippingCost value: raw='{}', normalized='{}'", val, normalized);
		throw new InvalidParameterException("Cannot parse ShippingCost value : " + val);
	}

	/**
	 * Normalises a raw shipping-cost string into a canonical form that can be
	 * compared to free-shipping tokens or parsed as a number.
	 *
	 * @param val raw value
	 * @return normalised string
	 */
	private static String normalize(String val) {
		String v = val.trim();

		// Strip Awin-style "COUNTRY:::" prefix (e.g. "FR:::4.99 EUR" -> "4.99 EUR").
		// This prefix appears in virtually every Awin feed row and previously caused an
		// InvalidParameterException for every single shipping-cost field.
		v = COUNTRY_PREFIX.matcher(v).replaceFirst("");

		String normalized = StringUtils.stripAccents(v)
				.toLowerCase()
				.replace(',', '.')
				.transform(s -> CURRENCY_SYMBOLS.matcher(s).replaceAll(""))
				.transform(s -> CURRENCY_WORDS.matcher(s).replaceAll(""))
				.transform(s -> KEEP_DECIMAL.matcher(s).replaceAll(" "))
				.trim();
		return StringUtils.normalizeSpace(normalized);
	}

	/**
	 * Returns {@code true} when the normalised value represents free shipping.
	 *
	 * @param normalized normalised value (lower-case, stripped of currency tokens)
	 * @return {@code true} if free shipping
	 */
	private static boolean isFreeShipping(String normalized) {
		return switch (normalized) {
			case "0", "0.0", "0.00", "offerte", "offert", "gratuit", "gratuite", "free", "free shipping",
					"livraison offerte", "livraison gratuite" -> true;
			default -> false;
		};
	}
}

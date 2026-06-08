package org.open4goods.commons.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * Parses merchant-provided shipping costs into an amount in the offer currency.
 */
public class ShippingCostParser {

	// TODO : Path from conf
	private static final Logger logger = GenericFileLogger.initLogger("product-shipping-cost-parser", Level.INFO, "/opt/open4goods/logs/");

	/**
	 * Parses a raw shipping cost.
	 *
	 * @param val raw merchant value
	 * @return parsed cost
	 * @throws InvalidParameterException when the value is empty or unknown
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

	private static String normalize(String val) {
		String normalized = StringUtils.stripAccents(val)
				.toLowerCase()
				.replace(',', '.')
				.replaceAll("[€$£]", "")
				.replaceAll("\\b(eur|euro|euros|usd|gbp|ttc|ht)\\b", "")
				.replaceAll("[^0-9.+\\-a-z ]", " ")
				.trim();
		return StringUtils.normalizeSpace(normalized);
	}

	private static boolean isFreeShipping(String normalized) {
		return switch (normalized) {
			case "0", "0.0", "0.00", "offerte", "offert", "gratuit", "gratuite", "free", "free shipping",
					"livraison offerte", "livraison gratuite" -> true;
			default -> false;
		};
	}
}

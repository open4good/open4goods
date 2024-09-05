package org.open4goods.commons.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

public class ShippingCostParser {

	// TODO : Path from conf
	private static final Logger logger = GenericFileLogger.initLogger("product-shipping-cost-parser", Level.INFO, "/opt/open4goods/logs/");

	public static Double parse(final String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate empty ShippingCost value");
		}

		if (!NumberUtils.isCreatable(val)) {
			return switch (val) {
			case "offerte" -> 0.0;
			default -> {
				logger.error("Unknown ProductCondition value : " + val);
				throw new InvalidParameterException("Cannot parse text ShippingCostvalue : " + val);
			}
			};

		} else {
			try {
				final String tmp = val.toLowerCase();
				return NumberUtils.createDouble(tmp);
			} catch (Exception e) {
				throw new InvalidParameterException("Cannot parse numeric ShippingCost value : " + val);
			}
		}
	}
}

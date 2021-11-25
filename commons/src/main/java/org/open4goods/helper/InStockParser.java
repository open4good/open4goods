package org.open4goods.helper;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.InStock;

/**
 * A simple parser to parse InStock values
 * @author Goulven.Furet
 *
 */
public class InStockParser {

	public static InStock parse(String val) throws InvalidParameterException {

		val = val.trim().toUpperCase();

		if (StringUtils.isNumeric(val) && Double.valueOf(val) > 1.00) {
			return InStock.INSTOCK;
		}

		switch (val) {
		case "1":
		case "TRUE":
		case "INSTOCK":
		case "HTTP://SCHEMA.ORG/INSTOCK":
		case "HTTPS://SCHEMA.ORG/INSTOCK":

			return InStock.INSTOCK;

		case "0":
		case "FALSE":
		case "HTTP://SCHEMA.ORG/OUTOFSTOCK":
		case "HTTP://SCHEMA.ORG/PREORDER":
		case "HTTP://SCHEMA.ORG/DISCONTINUED":
			return InStock.OUTOFSTOCK;

		case "UNKNOWN":
			return InStock.UNKNOWN;
		default:
			throw new InvalidParameterException("Cannot parse inStock value : " + val);

		}

	}
}

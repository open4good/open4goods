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

		if (null == val) {
			return null;
		}

		val = val.trim().toUpperCase();

		if (StringUtils.isNumeric(val) && Double.valueOf(val) > 1.00) {
			return InStock.INSTOCK;
		}

        return switch (val) {
            case "1", "TRUE", "INSTOCK","IN_STOCK", "AVAILABLE", "HTTP://SCHEMA.ORG/INSTOCK", "IN STOCK", "HTTPS://SCHEMA.ORG/INSTOCK" -> InStock.INSTOCK;
            case "0", "FALSE", "HTTP://SCHEMA.ORG/OUTOFSTOCK", "HTTP://SCHEMA.ORG/PREORDER", "HTTP://SCHEMA.ORG/DISCONTINUED" ->
                    InStock.OUTOFSTOCK;
            case "UNKNOWN" -> InStock.UNKNOWN;
            default -> throw new InvalidParameterException("Cannot parse inStock value : " + val);
        };

	}
}

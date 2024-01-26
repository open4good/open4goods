package org.open4goods.helper;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.InStock;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * A simple parser to parse InStock values
 * @author Goulven.Furet
 *
 */
public class InStockParser {

	// TODO : Path from conf
	private static final Logger logger = GenericFileLogger.initLogger("product-stock-parser", Level.INFO, "/opt/open4goods/logs/", false);

	
	public static InStock parse(String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			 throw new InvalidParameterException("Cannot evaluate empty InStock value");
		}

		val = val.trim().toUpperCase();

		if (StringUtils.isNumeric(val) && Double.valueOf(val) >= 1.00) {
			return InStock.INSTOCK;
		}

        return switch (val) {
            case "1", "TRUE", "INSTOCK","IN_STOCK", "AVAILABLE", "EN STOCK", "HTTP://SCHEMA.ORG/INSTOCK", "IN STOCK", "HTTPS://SCHEMA.ORG/INSTOCK" -> InStock.INSTOCK;
            case "0", "FALSE", "NON DISPONIBLE","OUT OF STOCK","OUTOFSTOCK", "HTTP://SCHEMA.ORG/OUTOFSTOCK", "HTTP://SCHEMA.ORG/PREORDER", "HTTP://SCHEMA.ORG/DISCONTINUED" ->
                    InStock.OUTOFSTOCK;
            case "UNKNOWN" -> InStock.UNKNOWN;
            default -> {
                logger.error("Unknown InStock value : " + val);
            	throw new InvalidParameterException("Cannot parse inStock value : " + val);
            }
        };

	}
}

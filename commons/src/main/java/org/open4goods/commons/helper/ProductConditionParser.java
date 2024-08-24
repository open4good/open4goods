package org.open4goods.commons.helper;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.model.constants.ProductCondition;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

public class ProductConditionParser {

	// TODO : Path from conf
	private static final Logger logger = GenericFileLogger.initLogger("product-condition-parser", Level.INFO, "/opt/open4goods/logs/", false);
	public static ProductCondition parse(String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate null or empty ProductCondition");
		}
			val = val.trim().toUpperCase();
			return switch (val) {
			    case "NEUF", "NEW", "PRODUIT NEUF", "NOUVEAU", "PRODUIT NEW", "HTTP://SCHEMA.ORG/NEWCONDITION" -> ProductCondition.NEW;
			    case "PRODUIT RECONDITIONNÉ", "RECONDITIONNÉ", "DEPACKAGED", "USED", "OCCASION", "VERY GOOD", "COLLECTION", "REFURBISHED", "GOOD", "FAIR", "EXCELLENT", "BON ÉTAT", "EXCELLENT ÉTAT", "TRÈS BON ÉTAT" -> ProductCondition.OCCASION;
			    default -> {   
                    logger.error("Unknown ProductCondition value : " + val);
			    	throw new InvalidParameterException("Cannot parse ProductCondition value : " + val);
			    }
			};

	}
}

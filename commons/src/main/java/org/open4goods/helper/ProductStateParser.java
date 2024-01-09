package org.open4goods.helper;

import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.ProductState;

public class ProductStateParser {

	public static ProductState parse(String val) throws InvalidParameterException {

		if (null == val) {
			throw new InvalidParameterException("Cannot parse null ProductState ");
		}
		val = val.trim().toUpperCase();

        return switch (val) {
            case "NEUF", "NEW", "PRODUIT NEUF", "NOUVEAU", "PRODUIT NEW", "HTTP://SCHEMA.ORG/NEWCONDITION" -> ProductState.NEW;
            case "PRODUIT RECONDITIONNÉ", "RECONDITIONNÉ", "USED", "OCCASION", "VERY GOOD", "COLLECTION", "REFURBISHED" -> ProductState.OCCASION;
            default -> {            	
            	throw new InvalidParameterException("Cannot parse ProductState value : " + val);
            }
        };

	}
}

package org.open4goods.helper;

import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.ProductState;

public class ProductStateParser {

	public static ProductState parse(String val) throws InvalidParameterException {

		if (null == val) {
			throw new InvalidParameterException("Cannot parse null ProductState ");
		}
		val = val.trim().toUpperCase();

		switch (val) {
		case "NEUF":
		case "NEW":
		case "PRODUIT NEUF":
		case "PRODUIT NEW":
		case "HTTP://SCHEMA.ORG/NEWCONDITION":
			return ProductState.NEW;

		case "PRODUIT RECONDITIONNÉ":
		case "USED":
		case "OCCASION":
		case "VERY GOOD":
		case "COLLECTION":	
			return ProductState.OCCASION;

		case "UNKNOWN":
		case "":
			return ProductState.UNKNOWN;

		default:
			throw new InvalidParameterException("Cannot parse ProductState value : " + val);

		}

	}
}

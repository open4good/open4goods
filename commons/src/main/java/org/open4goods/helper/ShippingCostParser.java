package org.open4goods.helper;

import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.exceptions.InvalidParameterException;

public class ShippingCostParser {

	public static Double parse(final String val) throws InvalidParameterException {

		final String tmp = val.toLowerCase();

		if (val.contains("offerte")) {
			return 0.0;
		}

		if (!NumberUtils.isCreatable(tmp)) {
			throw new InvalidParameterException("Not a numeric parsable shipping cost value : " + val);
		}

		return NumberUtils.createDouble(tmp);

	}
}

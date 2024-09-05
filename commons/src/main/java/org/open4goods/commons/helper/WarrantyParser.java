package org.open4goods.commons.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;



public class WarrantyParser {

	private static final Logger logger = GenericFileLogger.initLogger("product-warranty-parser", Level.INFO, "/opt/open4goods/logs/");
	public static Integer parse(final String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate empty Warranty value");
		}
		
		String tmp = val.toLowerCase();

//		tmp = tmp.replace("*", "");
//		tmp = tmp.replace("garantie", "");
		tmp = StringUtils.normalizeSpace(tmp);

		switch (tmp) {
		case "6 months", "6 mois":
			return 6;
		case "1 an", "12 months":
			return 12;
		case "2 ans","gartie 2":
			return 24;
		case "3 ans":
			return 36;
		case "4 ans":
			return 48;
		case "5 ans":
			return 60;
		case "6 ans":
			return 72;
		}


		if (!NumberUtils.isCreatable(tmp)) {
		}

		try {
			// Default numeric is year
			//TODO(feature, P1, 0.5) : Redesign through regexp
			return NumberUtils.createInteger(tmp)*12;
		} catch (Exception e) {
			logger.error("Unknown Warranty value : " + tmp);
			throw new InvalidParameterException("Unknown Warranty value: " + tmp);
		}
	}
}

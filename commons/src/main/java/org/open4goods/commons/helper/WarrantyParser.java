package org.open4goods.commons.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

public class WarrantyParser {

	private static final Logger logger = GenericFileLogger.initLogger("product-warranty-parser", Level.INFO, "/opt/open4goods/logs/");
	
	private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:an|ans|y|year|years|a)\\b");
	
	private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:m|mois|month|months)\\b");
	
	private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

	public static Integer parse(final String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate empty Warranty value");
		}
		
		String tmp = val.toLowerCase();
		tmp = StringUtils.normalizeSpace(tmp);

		if ("gartie 2".equals(tmp)) {
		    return 24;
		}

		Matcher monthMatcher = MONTH_PATTERN.matcher(tmp);
		if (monthMatcher.find()) {
			return (int) Math.round(Double.parseDouble(monthMatcher.group(1).replace(',', '.')));
		}
		
		Matcher yearMatcher = YEAR_PATTERN.matcher(tmp);
		if (yearMatcher.find()) {
			return (int) Math.round(Double.parseDouble(yearMatcher.group(1).replace(',', '.')) * 12);
		}

		Matcher numberMatcher = NUMBER_PATTERN.matcher(tmp);
		if (numberMatcher.find()) {
		    try {
				// Default numeric is year
				return (int) Math.round(Double.parseDouble(numberMatcher.group(1).replace(',', '.')) * 12);
			} catch (NumberFormatException e) {
				// Prevent NumberFormatException
			}
		}

		logger.error("Unknown Warranty value : " + tmp);
		throw new InvalidParameterException("Unknown Warranty value: " + tmp);
	}
}

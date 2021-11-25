package org.open4goods.services;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Gs1PrefixSchema {

	protected static final Logger logger = LoggerFactory.getLogger(Gs1PrefixSchema.class);

	private static final String SPLIT_CHAR = "–";
	private String country;
	private String range;

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * 
	 * @param countryNames
	 * @return All the effectiv gs1 association for a range notation
	 */
	Map<String, String> expand(Map<String, String> countryNames) {

		Map<String, String> ret = new HashMap<>();
		String code = countryNames.get(country.toLowerCase().trim());
		if (null == code) {
			logger.error("Cannot resolve country code for {}", country);
			return ret;
		}
		
		if (range.contains(SPLIT_CHAR)) {
			String[] frags = range.split(SPLIT_CHAR);

			Integer from = Integer.valueOf(frags[0].trim());
			Integer to = Integer.valueOf(frags[1].trim());

			for (int i = from.intValue(); i <= to; i++) {
					ret.put(StringUtils.leftPad(i + "", 3, '0'), code);
			}

		} else {
			code = countryNames.get(country.toLowerCase().trim());
			ret.put(range.trim(), code);			
		}

		logger.debug("Adding gs1 prefixes : {}", ret);
		return ret;
	}

}

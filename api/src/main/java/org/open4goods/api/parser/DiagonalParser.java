package org.open4goods.api.parser;

import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.attributes.AttributeParser;
import org.open4goods.exceptions.ParseException;
import org.open4goods.model.attribute.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse a diagonale
 *
 * @author goulven
 *
 */
public class DiagonalParser extends AttributeParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiagonalParser.class);

	private static String[] POUCE_TOKENS = { "POUCES", "POUCE","''","\"" };
	private static String[] CM_TOKENS = { "CM" };

	@Override
	/**
	 * Return inches for arbitrary diagonal definitions
	 */
	public String parse(String attrVal, final Attribute attribute, final AttributeConfig attributeConfig)
			throws ParseException {

		Double pouceVal = null, cmVal = null;

		attrVal = attrVal.replace(',', '.');


		// A try for
		//		"801 MM (MILLIMETERS)80.1 CM (CENTIMETERS)31.5354 IN (INCHES)2.628 FT (FEET)"



		int firstTryOffset = attrVal.indexOf("MM (MILLIMETERS)");

		if (firstTryOffset != -1) {
			final String firstTryVal = attrVal.substring(0,firstTryOffset).trim();
			if (NumberUtils.isNumber(firstTryVal)) {
				cmVal = Double.valueOf(firstTryVal) / 10.0;
			}
		}

		//		 (MILLIMÈTRES)
		if (null == cmVal) {
			firstTryOffset = attrVal.indexOf("MM (MILLIMÈTRES)");

			if (firstTryOffset != -1) {
				final String firstTryVal = attrVal.substring(0,firstTryOffset).trim();
				if (NumberUtils.isNumber(firstTryVal)) {
					cmVal = Double.valueOf(firstTryVal) / 10.0;
				}
			}
		}


		//		127 CM
		if (null == cmVal) {
			firstTryOffset = attrVal.indexOf(" CM");

			if (firstTryOffset != -1) {
				final String firstTryVal = attrVal.substring(0,firstTryOffset).trim();
				if (NumberUtils.isNumber(firstTryVal)) {
					cmVal = Double.valueOf( firstTryVal);
				}
			}
		}




		// A try for 65" (164 CM)
		if (null == cmVal) {
			firstTryOffset = attrVal.indexOf("\"");

			if (firstTryOffset != -1) {
				final String firstTryVal = attrVal.substring(0,firstTryOffset).trim();
				if (NumberUtils.isNumber(firstTryVal)) {
					pouceVal = Double.valueOf(firstTryVal);
				}
			}
		}

		// A try for 65''
		if (null == pouceVal) {
			firstTryOffset = attrVal.indexOf("'' ");

			if (firstTryOffset != -1) {
				final String firstTryVal = attrVal.substring(0,firstTryOffset).trim();
				if (NumberUtils.isNumber(firstTryVal)) {
					pouceVal = Double.valueOf(firstTryVal);
				}
			}
		}




		if (null == pouceVal && null == cmVal) {
			pouceVal = fromEnds(attrVal, "IN (INCHES)");
		}

		if (null == pouceVal) {
			pouceVal = fromEnds(attrVal, "\"");
		}


		//		24"
		if (null == pouceVal && null == cmVal) {

			if (NumberUtils.isNumber(attrVal)) {
				////////////////////////////
				// CASE 1 : A simple numeric
				// Have to look in attr title for an indication
				///////////////////////////
				final String title = attribute.getName();

				// Parsing as 'POUCE'
				for (final String t : POUCE_TOKENS) {
					if (title.contains(t)) {
						// This is a pouce value

						try {
							pouceVal = Double.valueOf(attrVal);

						} catch (final NumberFormatException e) {
							throw new ParseException("Was expecting a number for this 'POUCE' size" + attrVal);
						}
						break;
					}
				}

				// PArsing as 'CM'
				for (final String t : CM_TOKENS) {
					if (title.contains(t)) {
						// This is a pouce value

						try {
							cmVal = Double.valueOf(attrVal);

						} catch (final NumberFormatException e) {
							throw new ParseException("Was expecting a number for this 'CM' size" + attrVal);
						}
						break;
					}
				}

				if (null == cmVal && null == pouceVal) {
					throw new ParseException("Unable to determince pouce or inch from " + attrVal);

				}

			} else {

				// TODO(conf) : splitter from conf
				final String[] frags = attrVal.split("/");
				if (frags.length > 1) {
					if (frags.length > 2) {
						throw new ParseException("Too many fragments : " + attrVal);
					} else {
						////////////////////////////
						// CASE 2 : A probable pouces AND centimeter indication
						////////////////////////////

						Double tmp;
						for (final String frag : frags) {

							tmp = parsePouce(frag);
							if (null != tmp) {
								pouceVal = tmp;

							}
							tmp = parseCm(frag);

							if (null != tmp) {
								cmVal = tmp;
							}
						}
					}

				} else {
					////////////////////////////
					// CASE 3 : A probable pouces OR centimeter indication
					////////////////////////////
					Double tmp;

					tmp = parsePouce(attrVal);
					if (null != tmp) {
						pouceVal = tmp;

					}
					tmp = parseCm(attrVal);

					if (null != tmp) {
						cmVal = tmp;
					}

				}

			}

		}



		if (null != pouceVal) {
			attrVal = String.valueOf(pouceVal);
			return attrVal;
		} else if (null != cmVal) {
			attrVal = String.valueOf(cmVal / 2.54);
			return attrVal;
		} else {
			throw new ParseException("Cannot deduce cm or inches from " + attrVal);
		}






	}

	private Double fromEnds(final String source, final String remove) {

		if (! source.endsWith(remove)) {
			return null;
		}
		return fromContains(source, remove);
	}


	private Double fromContains(final String source, final String remove) {


		try {
			return Double.valueOf(source.replace(remove, ""));
		} catch (final NumberFormatException e) {
			return null;
		}
	}
	private Double parsePouce(String input) {

		for (final String frag : POUCE_TOKENS) {
			input = input.replaceFirst(frag, "");
		}

		try {
			return Double.valueOf(input);
		} catch (final NumberFormatException e) {
			return null;
		}

	}

	private Double parseCm(String input) {
		for (final String frag : CM_TOKENS) {
			input = input.replaceFirst(frag, "");
		}

		try {
			return Double.valueOf(input);
		} catch (final NumberFormatException e) {
			return null;
		}
	}

}

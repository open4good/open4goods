package org.open4goods.commons.helper;



import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;
//TODO(feature) : Shipping time could be greatly improved (jours ouvrés, inneficient parsing ..., log unresolved and so on for ShippingCostParser, ...)
public class ShippingTimeParser {

	private static final Logger logger = GenericFileLogger.initLogger("product-shipping-time-parser", Level.INFO, "/opt/open4goods/logs/", false);
	/**
	 * Parse a shipping time
	 * @param val
	 * @return
	 * @throws InvalidParameterException
	 */
	public static Integer parse(final String val) throws InvalidParameterException {

		String tmp = val.toLowerCase();

		tmp = tmp.trim();
		
		
		if (StringUtils.isEmpty(tmp)) {
			throw new InvalidParameterException("Cannot evaluate empty ShippingTime value");
		}

		switch (tmp) {

		case "livraison sous 3 à 5 jours":
			return 5;
		case "livraison sous 6 à 10 jours":
			return 10;
		case "sous 3 à 8 jours ouvrés":
			return 8;
		case "demain":
			//TODO(design) : a more general way to handle that
		case "sous 1 jour ouvré":
			return 1;

		case "livraison sous 72h":
			return 3;

		default:
			break;
		}
//
//		// Parsing as french full date
//		try {
//			//TODO(gof) : test / detect if we have a ending year before appending it
//			final LocalDate localDate = DateTimeFormat.forPattern( "EEEE dd MMM YYYY" ).withLocale( java.util.Locale.FRENCH ).parseLocalDate( tmp + " " + Year.now().getValue());
//
//			return Long.valueOf(localDate.toInterval().toDuration().getStandardDays()).intValue();
//
//		} catch (final Exception e) {
//			LOGGER.info("Cannot convert {} to a local french date : {}", tmp, e.getMessage());
//		}



		try {
			return NumberUtils.createInteger(tmp);
		} catch (Exception e) {
			throw new InvalidParameterException("Unknown ShippingTime value : " + tmp);
		}



	}
}

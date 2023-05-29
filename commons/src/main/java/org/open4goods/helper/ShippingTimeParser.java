package org.open4goods.helper;



import java.time.Year;

import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.open4goods.exceptions.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//TODO(feature) : Shipping time could be greatly improved (jours ouvrés, inneficient parsing ..., log unresolved and so on for ShippingCostParser, ...)
public class ShippingTimeParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ShippingTimeParser.class);
	/**
	 * Parse a shipping time
	 * @param val
	 * @return
	 * @throws InvalidParameterException
	 */
	public static Integer parse(final String val) throws InvalidParameterException {

		String tmp = val.toLowerCase();
		tmp = tmp.replace("en stock", "");

		tmp = tmp.trim();

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





		if (tmp.startsWith("dès")) {
			tmp = tmp.substring("dès".length()).trim();
		}




		// Parsing as french full date
		try {
			//TODO(gof) : test / detect if we have a ending year before appending it
			final LocalDate localDate = DateTimeFormat.forPattern( "EEEE dd MMM YYYY" ).withLocale( java.util.Locale.FRENCH ).parseLocalDate( tmp + " " + Year.now().getValue());

			return Long.valueOf(localDate.toInterval().toDuration().getStandardDays()).intValue();

		} catch (final Exception e) {
			LOGGER.info("Cannot convert {} to a local french date : {}", tmp, e.getMessage());
		}



		if (!NumberUtils.isNumber(tmp)) {
			throw new InvalidParameterException("Not a numeric parsable ShippingTime value : " + val);
		}

		return NumberUtils.createInteger(tmp);

	}
}

package org.open4goods.commons.model.constants;

import java.text.ParseException;

/** NOTE(gof) : complete here with other currencies will allow :
 * Currency conversion (if availlable at https://docs.openexchangerates.org/docs/supported-currencies)
 * Currency parsing (if symbols are presents in the "convert" fonction
 *
 * @author Goulven.Furet
 *
 */
public enum Currency {
	EUR, USD, CNY;
	public static Currency convert(final String currency) throws ParseException {


		switch (currency) {

		case "$":
			return Currency.USD;
		case "€":
			return Currency.EUR;

		default:

			try {

				return Currency.valueOf(currency);
			} catch (final Exception e) {
				throw new ParseException("Cannot parse Currency :" + currency + " : " + e.getMessage(), 0);
			}

		}

	}
}

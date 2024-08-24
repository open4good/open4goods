package org.open4goods.commons.services;

import org.open4goods.commons.model.constants.Currency;
import org.open4goods.commons.model.data.Price;
import org.open4goods.commons.model.data.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Goulven.Furet
 */
public abstract class StandardiserService {

	protected static final Logger logger = LoggerFactory.getLogger(StandardiserService.class);

	public static Currency DEFAULT_CURRENCY = Currency.EUR;

	public static Double DEFAULT_MAX_RATING = 5.0;


	public abstract void standarise(final Price price, Currency currency) ;

	public  void standarise(final Rating rating) {
		standariseRating(rating);
	}

	public  static void standariseRating(final Rating rating) {
		final Double max = rating.getMax();
		final Double value = rating.getValue();

		try {
			rating.setValue(value * DEFAULT_MAX_RATING / max);
			rating.setMax(DEFAULT_MAX_RATING);
		} catch (final Exception e) {
			logger.warn("Cannot standardize rating : {}",e.getMessage());
		}

	}



}

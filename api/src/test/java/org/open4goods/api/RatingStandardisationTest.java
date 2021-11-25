package org.open4goods.api;


import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.data.Price;
import org.open4goods.model.data.Rating;
import org.open4goods.services.StandardiserService;

public class RatingStandardisationTest {

	@Test
	public void testStandardisationRating() {

		final StandardiserService service = new StandardiserService() {

			@Override
			public void standarise(final Price price, final Currency c) {


			}};

			final Rating r = new Rating();

			r.setMax(5.0);
			r.setValue(2.0);

			service.standarise(r);

			if (StandardiserService.DEFAULT_MAX_RATING != 5) {
				fail("Expecting 5 for DEFAULT_MAX_RATING");


			}
			if (r.getValue() != 2.0 ) {
				fail("Should be 2 ");
			}

			r.setMax(100.0);
			r.setValue(50.0);

			service.standarise(r);
			if (r.getValue() != 2.5 ) {
				fail("Should be 2.5 ");
			}

	}

}

package org.open4goods.commons.config.yml.test;

import org.open4goods.commons.model.data.Rating;
import org.open4goods.commons.model.data.RatingType;

public class RatingsExpectedResult extends NumericExpectedResult {

	public void test(final Rating rating, final RatingType type, final TestResultReport report) {

		if (null == rating) {
			report.addMessage("No " + type + " rating");
			return;
		}
		testDouble(rating.getValue(), type.name(), report);

	}

}

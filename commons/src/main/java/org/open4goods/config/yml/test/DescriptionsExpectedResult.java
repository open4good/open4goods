package org.open4goods.config.yml.test;

import java.util.Set;

import org.open4goods.model.data.Description;

public class DescriptionsExpectedResult extends NumericExpectedResult {

	public void test(final Set<Description> descriptions, final TestResultReport ret) {
		testCollection(descriptions, "descriptions", ret);

	}


}

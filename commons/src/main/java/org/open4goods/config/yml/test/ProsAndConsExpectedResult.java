package org.open4goods.config.yml.test;

import java.util.Set;

import org.open4goods.model.data.ProsOrCons;

public class ProsAndConsExpectedResult extends NumericExpectedResult {

	public void test(final String string, final Set<ProsOrCons> pros, final TestResultReport ret) {
		testCollection(pros, string, ret);

	}

}

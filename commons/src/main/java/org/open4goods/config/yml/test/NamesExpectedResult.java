package org.open4goods.config.yml.test;

import java.util.Set;

public class NamesExpectedResult extends NumericExpectedResult{

	private String value;

	public void test(final Set<String> names, final TestResultReport ret) {

		testCollection(names, "names",ret);
		testNameEquals(names,ret);
	}


	public void testNameEquals (final Set<String> names, final TestResultReport ret) {


		if (null != value) {

			if (null == names || names.size() == 0) {
				ret.addMessage("Was expecting at least one name");
			}


			boolean found = false;
			for (final String name : names) {
				if (name.equals(value)) {
					found = true;
					break;

				}
			}

			if (!found) {
				ret.addMessage("Was expecting name " + value + ", we have : " + names);
			}
		}
	}


	public String getValue() {
		return value;
	}


	public void setValue(final String equals) {
		value = equals;
	}


}

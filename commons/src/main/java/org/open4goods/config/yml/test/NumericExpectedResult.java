package org.open4goods.config.yml.test;

import java.util.Collection;

public class NumericExpectedResult extends AbstractExpectedResult {

	private Double lowerThan;
	private Double greaterThan;
	private Double equals;
	public Double getLowerThan() {
		return lowerThan;
	}
	public void setLowerThan(final Double lowerThan) {
		this.lowerThan = lowerThan;
	}
	public Double getGreaterThan() {
		return greaterThan;
	}
	public void setGreaterThan(final Double greaterThan) {
		this.greaterThan = greaterThan;
	}
	public Double getEquals() {
		return equals;
	}
	public void setEquals(final Double mustEquals) {
		equals = mustEquals;
	}


	@SuppressWarnings("rawtypes")
	protected void testCollection(final Collection collection, final String itemName, final TestResultReport ret) {
		if (null == collection) {
			ret.addMessage("Missing : "+itemName);
			return;
		}

		testDouble((double) collection.size(), itemName, ret);
	}


	protected void testDouble(final Double dbl, final String itemName, final TestResultReport ret) {
		if (null == dbl) {
			ret.addMessage("Missing : "+itemName);
			return;
		}




		if (null != getGreaterThan()) {
			if (dbl < getGreaterThan()) {
				ret.addMessage("Was expecting at least " + getGreaterThan() + " "+itemName+", we have : " +dbl);
			}
		}


		if (null != getLowerThan()) {
			if (dbl > getLowerThan()) {
				ret.addMessage("Was expecting at most " + getLowerThan() + " "+itemName+", we have : " +dbl);
			}
		}

		if (null != equals) {
			if (dbl.doubleValue() != equals) {
				ret.addMessage("Was expecting exactly " + equals + " "+itemName+", we have : " +dbl);
			}
		}

	}


}

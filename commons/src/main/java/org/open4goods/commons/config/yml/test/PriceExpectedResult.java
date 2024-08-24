package org.open4goods.commons.config.yml.test;

import org.open4goods.commons.model.constants.Currency;
import org.open4goods.commons.model.constants.InStock;
import org.open4goods.commons.model.data.Price;

public class PriceExpectedResult extends NumericExpectedResult {

	private Currency currency;

	public void test(final Price price, final TestResultReport ret, final InStock inStock) {
		if (null == price) {
			ret.addMessage("Was expecting a price");
			return;
		}

		//
		//		if (null == inStock) {
		//			ret.addMessage("Was expecting a InStock value when price is set");
		//			return;
		//		}



		if (null != currency) {
			if (currency != price.getCurrency()) {
				ret.addMessage("Was expecting price currency " + currency + ", we have " + price.getCurrency());
			}

		}



		if (null != getGreaterThan()) {
			if (price.getPrice() < getGreaterThan()) {
				ret.addMessage("Price too low : was expecting at least " + getGreaterThan() + ", we have : " +price.getPrice());
			}
		}


		if (null != getLowerThan()) {
			if (price.getPrice() > getLowerThan()) {
				ret.addMessage("Price too high : as expecting at most " + getLowerThan() + " names, we have : " +price.getPrice());
			}
		}
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(final Currency currency) {
		this.currency = currency;
	}



}

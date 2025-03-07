package org.open4goods.model;

import java.util.Set;

import org.open4goods.model.price.Currency;

public interface Standardisable {

	Set<Standardisable> standardisableChildren();

	void standardize(StandardiserService standardiser, Currency currency);

}

package org.open4goods.model;

import java.util.Set;

import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;

public interface Standardisable {

	Set<Standardisable> standardisableChildren();

	void standardize(StandardiserService standardiser, Currency currency);

}

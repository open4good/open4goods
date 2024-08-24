package org.open4goods.commons.model;

import java.util.Set;

import org.open4goods.commons.model.constants.Currency;
import org.open4goods.commons.services.StandardiserService;

public interface Standardisable {

	Set<Standardisable> standardisableChildren();

	void standardize(StandardiserService standardiser, Currency currency);

}

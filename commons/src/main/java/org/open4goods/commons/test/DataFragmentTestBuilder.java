package org.open4goods.commons.test;

import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.constants.Currency;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Price;
import org.open4goods.commons.model.data.Rating;
import org.open4goods.commons.model.data.RatingType;
import org.open4goods.commons.model.data.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFragmentTestBuilder {

	private final static Logger log = LoggerFactory.getLogger(DataFragmentTestBuilder.class);
	private final DataFragment dataFragment;

	public DataFragmentTestBuilder(final DataFragment dataFragment) {
		this.dataFragment = dataFragment;
	}

	public DataFragmentTestBuilder brandUid(final String brandUid) {
		dataFragment.addReferentielAttribute(ReferentielKey.MODEL.toString(), brandUid);
		return this;
	}


	public DataFragmentTestBuilder tag(final String tag) {
		dataFragment.addProductTag(tag);
		return this;
	}

	public DataFragmentTestBuilder gtin(final String gtin) {
		dataFragment.addReferentielAttribute(ReferentielKey.GTIN.toString(), gtin);
		return this;
	}

	public DataFragmentTestBuilder url(final String url) {
		dataFragment.setUrl(url);
		return this;
	}

	public DataFragmentTestBuilder affiliatedUrl(final String url) {
		dataFragment.setAffiliatedUrl(url);
		return this;
	}


	public DataFragmentTestBuilder resource(final Resource r) {
		try {
			dataFragment.addResource(r);
		} catch (final ValidationException e) {
			e.printStackTrace();
		}
		return this;
	}

	public DataFragmentTestBuilder resource(final String url, final String key) {
		final Resource r = new Resource();
		r.setCacheKey(key);
		r.setUrl(url);
		r.setTimeStamp(System.currentTimeMillis());
		try {
			dataFragment.addResource(r);
		} catch (final ValidationException e) {
			e.printStackTrace();
		}
		return this;
	}

	public DataFragmentTestBuilder resource(final String url) {
		resource(url, null);

		return this;
	}

	/**
	 * Set (or invert) the referentielData flag
	 *
	 * @return
	 */
	public DataFragmentTestBuilder referentiel() {
		dataFragment.setReferentielData(!dataFragment.getReferentielData());
		return this;
	}


	public DataFragmentTestBuilder rating(final String storeName, final RatingType type, final Double value, final Long numberOfVoters, final Integer min, final Double max) {

		final Rating r = new Rating();
		r.setMin(min);
		r.setMax(max);
		r.setNumberOfVoters(numberOfVoters);
		r.addTag(type);
		r.setValue(value);
		try {
			dataFragment.addRating(r);
		} catch (final ValidationException e) {
			log.error("Cannot add rating : {}",e.getMessage());
		}
		return this;
	}

	public DataFragmentTestBuilder store(final String storeName) {
		dataFragment.setDatasourceName(storeName);
		return this;
	}

	public DataFragmentTestBuilder brand(final String brand) {
		dataFragment.addReferentielAttribute(ReferentielKey.BRAND.toString(), brand);
		return this;
	}

	public DataFragmentTestBuilder attribute(final String name, final String value) {
		dataFragment.addAttribute(name, value, "fr", null);
		return this;
	}


	//	public DataFragmentTestBuilder related(final String relationAttribute, final String relationId) {
	//		dataFragment.setQualifier(relationId);
	//		dataFragment.setRelationIdAttribute(relationAttribute);
	//
	//		return this;
	//	}

	public DataFragmentTestBuilder indexationDate(final long l) {
		dataFragment.setLastIndexationDate(l);
		return this;
	}

	public DataFragment build() {
		return dataFragment;
	}

	public DataFragmentTestBuilder price(final double price, final Currency cur) {
		dataFragment.setPrice(new Price(price,cur));
		return this;
	}





}

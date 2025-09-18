package org.open4goods.b2b.service;

import java.util.Locale;

import org.open4goods.model.product.Product;

import model.facets.AbstractFacet;

/**
 * Contract a facet converter must implement
 */
public interface FacetServiceInterface {
	/**
	 *
	 * @return the cost (in credits) for this facet
	 */
	public short getCreditsCost();

	/**
	 * Transform a product to a facet, given a specified locale
	 * @param product
	 * @param locale
	 * @return
	 */
	public AbstractFacet render (Product product, Locale locale);

}

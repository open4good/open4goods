package org.open4goods.b2b.service.facets;

import java.util.Locale;

import org.open4goods.b2b.service.FacetServiceInterface;
import org.open4goods.model.product.Product;

import model.facets.PriceFacet;

public class PriceFacetService implements FacetServiceInterface{

	// How many credits this facet cost ?
	private static final short FACET_PRICE = 1;

	@Override
	public short getCreditsCost() {
		return FACET_PRICE;
	}

	@Override
	public PriceFacet render(Product product, Locale locale) {
		PriceFacet priceFacet = new PriceFacet();
		priceFacet.setPrice(1234f);
		priceFacet.setCurrency("€");

		return priceFacet;
	}

}

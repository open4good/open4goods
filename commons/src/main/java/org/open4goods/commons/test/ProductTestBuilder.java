package org.open4goods.commons.test;

import org.open4goods.commons.model.product.LegacyProduct;
public class ProductTestBuilder {

	private final LegacyProduct aggregatedData;



	public ProductTestBuilder(final LegacyProduct aggregatedData) {
		this.aggregatedData = aggregatedData;
	}


	//	public ProductTestBuilder resourceKey(final String key) {
	//		aggregatedData.getResourceKeys().add(key);
	//		return this;
	//	}

	//	public ProductTestBuilder attribute(final String key, final Object value) {
	//
	//		final Attribute a = new Attribute();
	//		a.setName(key);
	//		a.setRawValue(value);
	//		aggregatedData.getAttributes().put(key,a);
	//
	//		return this;
	//	}


	public LegacyProduct build() {
		return aggregatedData;
	}




}

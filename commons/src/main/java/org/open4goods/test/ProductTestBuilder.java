package org.open4goods.test;

import org.open4goods.model.product.AggregatedData;
public class ProductTestBuilder {

	private final AggregatedData aggregatedData;



	public ProductTestBuilder(final AggregatedData aggregatedData) {
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


	public AggregatedData build() {
		return aggregatedData;
	}




}

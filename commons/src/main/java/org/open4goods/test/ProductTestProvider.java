package org.open4goods.test;

import java.util.UUID;

import org.open4goods.model.product.AggregatedData;

public class ProductTestProvider {




	public static  ProductTestBuilder empty() {
		final ProductTestBuilder p = new ProductTestBuilder(new AggregatedData());
		return p;
	}

	public static  ProductTestBuilder defaulted() {
		final AggregatedData p = new AggregatedData();
		p.setId(UUID.randomUUID().toString());

		p.getAlternativeIds().add("alternateId1");
		p.getAlternativeIds().add("alternateId2");
		p.getAlternativeIds().add("alternateId3");



		return new ProductTestBuilder(p);


	}


	public static  ProductTestBuilder random() {
		//TODO(gof) : implement randomisation
		return defaulted();
	}



}

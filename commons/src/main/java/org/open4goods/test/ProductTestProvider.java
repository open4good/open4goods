package org.open4goods.test;

import java.util.UUID;

import org.open4goods.model.product.Product;

public class ProductTestProvider {




	public static  ProductTestBuilder empty() {
		final ProductTestBuilder p = new ProductTestBuilder(new Product());
		return p;
	}

	public static  ProductTestBuilder defaulted() {
		final Product p = new Product();
		p.setId(UUID.randomUUID().toString());

//		p.getAlternativeIds().add(new UnindexedKeyValTimestamp("provider 1", "alternateId1")); 
//		p.getAlternativeIds().add(new UnindexedKeyValTimestamp("provider 2", "alternateId2"));
//		p.getAlternativeIds().add(new UnindexedKeyValTimestamp("provider 3", "alternateId3"));

		return new ProductTestBuilder(p);

	}


	public static  ProductTestBuilder random() {
		//TODO(gof) : implement randomisation
		return defaulted();
	}



}

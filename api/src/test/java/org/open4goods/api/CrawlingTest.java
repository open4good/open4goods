package org.open4goods.api;

import org.junit.jupiter.api.Test;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.SerialisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public class CrawlingTest {
	
	@Autowired
	private SerialisationService serialisationService;

	@Autowired
	private ProductRepository productRepository;

	@Test
	public void registrationWorksThroughAllLayers() throws Exception {

		productRepository.countMainIndex();

	}

	@Test
	public void whenContextLoads_thenServiceISNotNull() {
		System.out.println(productRepository.countMainIndex());
		Assert.isTrue(productRepository.countMainIndex() == 0);
	}

	

}

package org.open4goods.crawler;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.crawler.services.fetching.AwinCatalogService;
import org.open4goods.exceptions.ValidationException;

public class AwinCatalogServiceTest {

	@Test
	public void test() {

		//TODO : in hidden conf
		AwinCatalogService service = new AwinCatalogService("https://productdata.awin.com/datafeed/list/apikey/f3fe5d60a3668a0256f1dcdf9956ac03");

		
		service.loadCatalog();
		
		assertTrue(service.getEntries().size()>3);
		

	}

}

package org.open4goods.commons;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.helper.IdHelper;
import org.open4goods.services.GoogleTaxonomyService;

public class TaxonomyTest {

	@Test
	public void testLoad() {

		GoogleTaxonomyService tService = new GoogleTaxonomyService();
		
		try {
			tService.loadFile("/taxonomy/taxonomy-with-ids.fr-FR.csv", "fr");
//			tService.loadFile("/taxonomy/taxonomy-with-ids.en-US.csv", "en");
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
	}
//	
	
	

	
	

}

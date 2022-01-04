package org.open4goods.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.services.GoogleTaxonomyService;

class TaxonomyTest {

	@Test
	void test() {
		GoogleTaxonomyService taxonomyService = new GoogleTaxonomyService();
		
		try {
			// Loading fr
			taxonomyService.loadFile("/taxonomy/taxonomy-with-ids.fr-FR.csv", "fr");
			
			assertTrue(taxonomyService.getLastCategoriesId().get("fr"). size() > 5500 );
			assertEquals(taxonomyService.getTaxonomyId("fr", "Sous-ventrière"), 1491);						
			assertTrue(taxonomyService.getParentsCategories("fr", 1491).size() ==5) ;
			assertEquals(taxonomyService.getCategory("fr", 1491), "Sous-ventrière");
			
			
			// Loading en
			taxonomyService.loadFile("/taxonomy/taxonomy-with-ids.en-US.csv", "en");
						
			assertTrue(taxonomyService.getLastCategoriesId().get("en"). size() > 5500 );
			assertEquals(taxonomyService.getTaxonomyId("en", "Pet Fragrances & Deodorizing Sprays"), 503733);						
			assertTrue(taxonomyService.getParentsCategories("en", 503733).size() ==4) ;
			assertEquals(taxonomyService.getCategory("en", 503733), "Pet Fragrances & Deodorizing Sprays");
			
			
			// Re operate fr tests
			assertTrue(taxonomyService.getLastCategoriesId().get("fr"). size() > 5500 );
			assertEquals(taxonomyService.getTaxonomyId("fr", "Sous-ventrière"), 1491);
			assertEquals(taxonomyService.getCategory("fr", 1491), "Sous-ventrière");
			
			assertTrue(taxonomyService.getParentsCategories("fr", 1491).size() ==5) ;
			
		} catch (IOException e) {
			e.printStackTrace();
			fail (e.getMessage());
		}
	}

}

package org.open4goods.commons;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.config.TestConfig;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestConfig.class)
public class GoogleTaxonomyServiceTest {

	@Autowired
    private GoogleTaxonomyService gts;

    
    @Test
    public void testLoadFile() throws IOException, InvalidParameterException {
        
        // Call the loadFile method
        gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.fr-FR.txt", "fr");
        gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.fr-CH.txt", "fr");
        
        
        // Primary resolution
        int id = gts.resolve("Appareils électroniques > Réseaux");
        assertTrue(id == 342);
        
        // Null / empty check
        assertTrue(gts.resolve("") == null);
        assertTrue(gts.resolve(null) == null);        
        assertTrue(gts.resolve("sc<;w,x; <mwL?XMQSL>C?ML>W") == null);
        
        
       // Checking the deep resolution
        
        gts.getLocalizedTaxonomy();
        
        // 505767
        int deep4 = gts.resolve("Appareils électroniques > Accessoires électroniques > Composants d'ordinateur > Périphériques de stockage > Accessoires pour disques durs > Boîtiers et fixations pour disques durs");
        assertTrue(deep4 == 505767);
       
        // 276
        int deep3 = gts.resolve("Appareils électroniques > Accessoires électroniques > Alimentation > Piles");
        assertTrue(deep3 == 276);

        
        // 3895
        int deep2 = gts.resolve("Appareils électroniques > Accessoires pour GPS");
        assertTrue(deep2 == 3895);

        
        // 222
        int deep1 = gts.resolve("Appareils électroniques");
        assertTrue(deep1 == 222);
        
        assertTrue(gts.selectDeepest("fr", 505767, 276, 3895, 222)== 505767);
      
        
        // With a buggy id
        assertTrue(gts.selectDeepest("fr", 279, 222, 276)== 276);
        
        
        assertTrue(gts.selectDeepest("fr", 3895, 222)== 3895);
        
        assertTrue(gts.selectDeepest("fr",  222)== 222);
        
        
        
       
    }
}
package org.open4goods.commons;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.config.TestConfig;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.IcecatFeatureService;
import org.open4goods.services.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestConfig.class)
public class IcecatFeaturesServiceTest {

	@Autowired private IcecatFeatureService featureService;

    
//    @Test
//    public void testLoadFile() throws IOException, InvalidParameterException {
//        
//       
//    	
//    	
//    	featureService.loadFeatures();
//        
//    	
//    	assertTrue(featureService.resolve("COULEUR").equals(""));
//        
//       
//    }
}
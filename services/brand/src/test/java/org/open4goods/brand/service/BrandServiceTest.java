package org.open4goods.brand.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.brand.model.Brand;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

class BrandServiceTest {

    private BrandService brandService;
    private RemoteFileCachingService remoteFileCachingMock;
    private SerialisationService serialisationServiceMock;

    @BeforeEach
    void setUp() throws Exception {
        remoteFileCachingMock = mock(RemoteFileCachingService.class);
        serialisationServiceMock = mock(SerialisationService.class);

        // Subclass to avoid network call in constructor
        brandService = new BrandService(remoteFileCachingMock, serialisationServiceMock) {
            @Override
            protected void loadBrandMappings() {
                // Manually populate some brands for testing
                // We access the private map if possible, or we just rely on resolve working if we could verify it.
                // But wait, resolve uses brandsByName which is private.
                // We need to verify how to populate brandsByName for test without reflection?
                // Actually loadBrandMappings populates brandsByName. 
                // We can't access brandsByName directly from here unless we use reflection or if we put this test in same package.
                // This test IS in same package (org.open4goods.brand.service).
                // But brandsByName is private.
                // We can use reflection to populate it in this override.
            }
            
            // Allow injecting test data
            public void setTestBrand(String name, String company) {
                 // But we can't access brandsByName (private).
                 // We will rely on aliases testing. 
                 // If resolve uses aliases, it should work even if brand not in map (it returns new Brand(alias)).
                 // But we want to map "LG ELECTRONICS" -> "LG" -> "LG Electronics, Inc."
                 // So we need "LG" to be in the map.
            }
        };
        
        // Reflection to inject "LG" into brandsByName
        java.lang.reflect.Field field = BrandService.class.getDeclaredField("brandsByName");
        field.setAccessible(true);
        Map<String, Brand> map = (Map<String, Brand>) field.get(brandService);
        Brand lg = new Brand("LG");
        lg.setCompanyName("LG Electronics, Inc.");
        map.put("LG", lg);
    }

    @Test
    void testResolveWithAlias() {
        // Without alias, it should fail (return brand with input name)
        Brand resolved = brandService.resolve("LG ELECTRONICS");
        // Expectation: It returns "LG ELECTRONICS" as brand name (unresolved)
        // If it was resolved to LG, it should have company name "LG Electronics, Inc."
        
        // This assertion confirms current behavior (FAILING requirement)
        // assertEquals("LG Electronics, Inc.", resolved.getCompanyName()); 
        
        // We assert what we expect to FAIL currently
        // Currently: "LG ELECTRONICS" -> Sanitized "LG ELECTRONICS" -> Not found -> New Brand("LG ELECTRONICS") -> No company name.
        
        assertEquals(null, resolved.getCompanyName(), "Currently should be null/unknown");

        // Now we want to assert that AFTER fix, it works. 
        Map<String, String> aliases = new HashMap<>();
        aliases.put("LG ELECTRONICS", "LG");
        brandService.setBrandsAlias(aliases);
        
        resolved = brandService.resolve("LG ELECTRONICS");
        assertEquals("LG Electronics, Inc.", resolved.getCompanyName());
    }
}

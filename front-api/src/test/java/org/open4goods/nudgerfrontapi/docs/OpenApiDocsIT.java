package org.open4goods.nudgerfrontapi.docs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.kohsuke.github.GHRepository;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.services.geocode.service.IpGeolocationService;


@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc

class OpenApiDocsIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GHRepository ghRepository;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private IpGeolocationService ipGeolocationService;

    @MockitoBean
    private IcecatFeatureRepository icecatFeatureRepository;

    @MockitoBean
    private IcecatCategoryRepository icecatCategoryRepository;

    @MockitoBean
    private IcecatFeatureGroupRepository icecatFeatureGroupRepository;

    @MockitoBean
    private IcecatSupplierRepository icecatSupplierRepository;

    private static final String SHARED_TOKEN = "test-token";

    @Test
    void legacyDocumentationPathIsRejected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void frontendApiDocsArePublicForClientGeneration() throws Exception {
        mockMvc.perform(get("/v3/api-docs/front")
                .header("X-Shared-Token", SHARED_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/products'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/auth/google'].post").exists());
    }
}

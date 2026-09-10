package org.open4goods.nudgerfrontapi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.services.geocode.service.IpGeolocationService;
import org.open4goods.model.RolesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests verifying the {@link SharedTokenFilter} behaviour.
 */
@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc

class SharedTokenFilterIT {

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

    /**
     * Shared token used for authenticated requests in tests.
     */
    private static final String SHARED_TOKEN = "test-token";

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/products/fields/sortable")
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongTokenIsRejected() throws Exception {
        mockMvc.perform(get("/products/fields/sortable")
                .header("X-Shared-Token", "wrong")
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenAllowsAccess() throws Exception {
        mockMvc.perform(get("/products/fields/sortable")
                .header("X-Shared-Token", SHARED_TOKEN)
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());
    }
}

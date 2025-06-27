package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.commons.services.ProductNameSelectionService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository repository;
    @MockBean
    private VerticalsConfigService configService;
    @MockBean
    private ProductNameSelectionService nameService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getBestNameReturnsValue() throws Exception {
        Product product = new Product();
        product.setId(42L);
        product.setOfferNames(Set.of("Name1", "BestName"));

        when(repository.getById(42L)).thenReturn(product);
        when(nameService.selectBestNameIndustrial(anyList())).thenReturn(Optional.of("BestName"));

        mockMvc.perform(get("/product/bestname").param("gtin", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string("BestName"));
    }
}


package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.services.ProductNameSelectionService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class LegacyApiProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @Mock
    private ProductNameSelectionService productNameSelectionService;

    @BeforeEach
    public void setup() {
        LegacyApiProductController controller = new LegacyApiProductController(productRepository, verticalsConfigService, productNameSelectionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getRandomProducts() throws Exception {
        when(productRepository.getRandomProducts(anyString(), anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/product/random")
                .param("number", "5")
                .param("vertical", "tv")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(productRepository).getRandomProducts("tv", 5);
    }
}

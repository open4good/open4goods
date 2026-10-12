package org.open4goods.api.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetriksController.class)
@ContextConfiguration(classes = MetriksController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MetriksControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private GoogleSearchConsoleService searchConsoleService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSystemMetrics() throws Exception {
        mockMvc.perform(get("/api/metriks/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].id").value("system.disk.total"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetFunctionalMetrics() throws Exception {
        Set<AffiliationPartner> partners = new HashSet<>();
        AffiliationPartner p1 = new AffiliationPartner();
        p1.setId("p1");
        partners.add(p1);
        AffiliationPartner p2 = new AffiliationPartner();
        p2.setId("p2");
        partners.add(p2);

        when(productRepository.countMainIndex()).thenReturn(1234L);
        when(productRepository.countMainIndexValidAndRated()).thenReturn(99L);
        when(feedService.getFeedsUrl()).thenReturn(new HashSet<>());
        when(feedService.getPartners()).thenReturn(partners);

        mockMvc.perform(get("/api/metriks/functional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].id").value("business.products.total"))
                .andExpect(jsonPath("$.events[0].value").value(1234))
                .andExpect(jsonPath("$.events[1].id").value("business.products.rated"))
                .andExpect(jsonPath("$.events[1].value").value(99));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetrics() throws Exception {
        GoogleSearchConsoleService.PagesWithImpressionsResult result =
                new GoogleSearchConsoleService.PagesWithImpressionsResult(
                        5000L, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 8), false);
        when(searchConsoleService.countPagesWithImpressions()).thenReturn(result);

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events[0].id").value("seo.gsc.pages_with_impressions"))
                .andExpect(jsonPath("$.events[0].value").value(5000))
                .andExpect(jsonPath("$.events[0].params.windowStartDate").value("2026-09-11"))
                .andExpect(jsonPath("$.events[0].params.windowEndDate").value("2026-10-08"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetricsOnErrorReturnsErrorEvent() throws Exception {
        when(searchConsoleService.countPagesWithImpressions())
                .thenThrow(new java.io.IOException("Search Console API HTTP 500: boom"));

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].id").value("seo.gsc.pages_with_impressions"))
                .andExpect(jsonPath("$.events[0].status").value("error"))
                .andExpect(jsonPath("$.events[0].errorMessage").value("Search Console API HTTP 500: boom"));
    }
}

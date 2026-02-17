package org.open4goods.api.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.services.feedservice.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetriksController.class)
@ContextConfiguration(classes = MetriksController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MetriksControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedService feedService;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

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

        when(feedService.getPartners()).thenReturn(partners);

        mockMvc.perform(get("/api/metriks/functional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].id").value("business.partners.count"))
                .andExpect(jsonPath("$.events[0].value").value(2));
    }
}

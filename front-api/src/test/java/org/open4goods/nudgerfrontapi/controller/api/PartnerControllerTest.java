package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.dto.partner.AffiliationPartnerDto;
import org.open4goods.nudgerfrontapi.service.AffiliationPartnerService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link PartnerController}.
 */
@ExtendWith(MockitoExtension.class)
class PartnerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AffiliationPartnerService affiliationPartnerService;

    @BeforeEach
    void setUp() {
        PartnerController controller = new PartnerController(affiliationPartnerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldExposeAffiliationPartners() throws Exception {
        List<AffiliationPartnerDto> partnerDtos = List.of(
                new AffiliationPartnerDto("p1", "Partner", "https://aff.example/p1",
                        "https://portal.example/p1", "https://cdn.example/logo/Partner",
                        "https://cdn.example/favicon?url=Partner", List.of("FR"))
        );
        when(affiliationPartnerService.getPartnerDtos()).thenReturn(partnerDtos);

        mockMvc.perform(get("/partners/affiliation").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$[0].id").value("p1"))
                .andExpect(jsonPath("$[0].faviconUrl").value("https://cdn.example/favicon?url=Partner"));
    }
}


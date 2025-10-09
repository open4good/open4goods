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
import org.open4goods.nudgerfrontapi.config.properties.EcosystemPartnersProperties;
import org.open4goods.nudgerfrontapi.config.properties.MentorPartnersProperties;
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

    private EcosystemPartnersProperties ecosystemPartnersProperties;
    private MentorPartnersProperties mentorPartnersProperties;

    @BeforeEach
    void setUp() {
        ecosystemPartnersProperties = new EcosystemPartnersProperties();
        EcosystemPartnersProperties.Partner ecosystemPartner = new EcosystemPartnersProperties.Partner();
        ecosystemPartner.setName("French Tech Ouest");
        ecosystemPartner.setBlocId("pages:ecosystem:french-tech");
        ecosystemPartner.setUrl("https://www.ft-brestbretagneouest.bzh/");
        ecosystemPartner.setImageUrl("/images/ecosystem/frenchTech.jpeg");
        ecosystemPartnersProperties.setPartners(List.of(ecosystemPartner));

        mentorPartnersProperties = new MentorPartnersProperties();
        MentorPartnersProperties.Partner mentorPartner = new MentorPartnersProperties.Partner();
        mentorPartner.setName("Moovance");
        mentorPartner.setBlocId("pages:partners:moovance");
        mentorPartner.setUrl("https://www.moovance.fr/");
        mentorPartner.setImageUrl("/images/mentors/moovance.jpeg");
        mentorPartnersProperties.setPartners(List.of(mentorPartner));

        PartnerController controller = new PartnerController(affiliationPartnerService,
                ecosystemPartnersProperties, mentorPartnersProperties);
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

    @Test
    void shouldExposeEcosystemPartnersFromProperties() throws Exception {
        mockMvc.perform(get("/partners/ecosystem").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$[0].name").value("French Tech Ouest"))
                .andExpect(jsonPath("$[0].blocId").value("pages:ecosystem:french-tech"))
                .andExpect(jsonPath("$[0].url").value("https://www.ft-brestbretagneouest.bzh/"))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/ecosystem/frenchTech.jpeg"));
    }

    @Test
    void shouldExposeMentorPartnersFromProperties() throws Exception {
        mockMvc.perform(get("/partners/mentors").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$[0].name").value("Moovance"))
                .andExpect(jsonPath("$[0].blocId").value("pages:partners:moovance"))
                .andExpect(jsonPath("$[0].url").value("https://www.moovance.fr/"))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/mentors/moovance.jpeg"));
    }
}


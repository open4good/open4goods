package org.open4goods.nudgerfrontapi.controller.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.TeamProperties;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link TeamController}.
 */
class TeamControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TeamProperties properties = new TeamProperties();

        TeamProperties.Member core = new TeamProperties.Member();
        core.setName("Jane Doe");
        core.setLinkedInUrl("https://www.linkedin.com/in/janedoe/");
        core.setImageUrl("/img/jane.jpeg");

        TeamProperties.Member contributor = new TeamProperties.Member();
        contributor.setName("John Roe");
        contributor.setLinkedInUrl("https://www.linkedin.com/in/johnroe/");
        contributor.setImageUrl("/img/john.jpeg");

        properties.setCores(List.of(core));
        properties.setContributors(List.of(contributor));

        mockMvc = MockMvcBuilders.standaloneSetup(new TeamController(properties))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldExposeConfiguredTeam() throws Exception {
        mockMvc.perform(get("/team").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cores[0].name").value("Jane Doe"))
                .andExpect(jsonPath("$.contributors[0].imageUrl").value("/img/john.jpeg"));
    }
}

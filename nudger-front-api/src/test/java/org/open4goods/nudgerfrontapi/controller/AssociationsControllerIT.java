package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import org.open4goods.nudgerfrontapi.dto.AssociationDto;
import org.open4goods.nudgerfrontapi.service.AssociationsService;

@SpringBootTest
@AutoConfigureMockMvc
class AssociationsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssociationsService associationsService;

    @Test
    void associationsEndpointReturnsBodyAndCacheHeader() throws Exception {
        given(associationsService.getAssociations()).willReturn(List.of(new AssociationDto("id", "name")));

        mockMvc.perform(get("/associations").with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value("id"))
               .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age")));
    }
}

package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.HomeCompositeResponse;
import org.open4goods.nudgerfrontapi.dto.PartnerDto;
import org.open4goods.nudgerfrontapi.dto.StatsDto;
import org.open4goods.nudgerfrontapi.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeService homeService;

    @Test
    void homeEndpointReturnsBodyAndCacheHeader() throws Exception {
        HomeCompositeResponse response = new HomeCompositeResponse(new StatsDto(1, 2), List.of(new PartnerDto("name", "url")));
        given(homeService.getHome()).willReturn(response);

        mockMvc.perform(get("/home").with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.stats.products").value(1))
               .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age")));
    }
}

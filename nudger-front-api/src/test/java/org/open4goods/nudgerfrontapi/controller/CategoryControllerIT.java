package org.open4goods.nudgerfrontapi.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void categoriesEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/categories").with(jwt()))
               .andExpect(status().isOk());
    }

    @Test
    void categoryByIdEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", 1).with(jwt()))
               .andExpect(status().isOk());
    }
}

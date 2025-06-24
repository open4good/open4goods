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
class PostsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listPosts() throws Exception {
        mockMvc.perform(get("/posts").with(jwt()))
            .andExpect(status().isOk());
    }

    @Test
    void getPost() throws Exception {
        mockMvc.perform(get("/posts/hello-world").with(jwt()))
            .andExpect(status().isOk());
    }
}

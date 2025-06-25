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
import java.util.Date;
import java.util.List;
import org.open4goods.nudgerfrontapi.dto.BlogPostDto;
import org.open4goods.nudgerfrontapi.service.BlogService;

@SpringBootTest
@AutoConfigureMockMvc
class PostsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogService blogService;

    @Test
    void listPosts() throws Exception {
        given(blogService.getPosts(null)).willReturn(List.of(new BlogPostDto("slug", "title", "en", "s", "b", new Date())));

        mockMvc.perform(get("/posts").with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].slug").value("slug"))
               .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age")));
    }

    @Test
    void getPost() throws Exception {
        given(blogService.getPost("hello-world", null)).willReturn(new BlogPostDto("hello-world", "title", "en", "s", "b", new Date()));

        mockMvc.perform(get("/posts/hello-world").with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.slug").value("hello-world"))
               .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age")));
    }
}

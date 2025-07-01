package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.controller.api.PostsController;
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.services.blog.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "front.cache.path=${java.io.tmpdir}")
@AutoConfigureMockMvc
class PostsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostsController controller;

    @MockBean
    private BlogService blogService;

    @Test
    void postsEndpointReturnsList() throws Exception {
        BlogPost post = new BlogPost();
        post.setTitle("Title");
        post.setUrl("slug");
        given(blogService.getPosts(any())).willReturn(List.of(post));

        mockMvc.perform(get("/contents/posts").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.data[0].title").value("Title"));
    }

    @Test
    void postEndpointReturns404WhenMissing() throws Exception {
        given(blogService.getPostsByUrl()).willReturn(Map.of());

        mockMvc.perform(get("/contents/posts/{slug}", "missing").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postEndpointReturnsData() throws Exception {
        BlogPost post = new BlogPost();
        post.setTitle("Title");
        post.setUrl("slug");
        post.setCreated(new Date(1L));
        given(blogService.getPostsByUrl()).willReturn(Map.of("slug", post));

        mockMvc.perform(get("/contents/posts/{slug}", "slug").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("slug"))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void tagsEndpointReturnsMap() throws Exception {
        Map<String, Integer> tags = new LinkedHashMap<>();
        tags.put("eco", 2);
        given(blogService.getTags()).willReturn(tags);

        mockMvc.perform(get("/blog/tags").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("eco"))
                .andExpect(jsonPath("$[0].count").value(2));
    }
}

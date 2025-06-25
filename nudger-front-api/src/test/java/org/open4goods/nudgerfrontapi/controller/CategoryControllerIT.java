package org.open4goods.nudgerfrontapi.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void categoriesEndpointReturnsList() throws Exception {
        var dto = new CategoryService.CategoryDto(1, Map.of("en", "root"), "vert", List.of());
        given(categoryService.listRootCategories(false)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/categories").with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total").value(1))
               .andExpect(jsonPath("$.items[0].id").value(1));
    }

    @Test
    void categoryByIdEndpointReturnsDto() throws Exception {
        var dto = new CategoryService.CategoryDto(1, Map.of("en", "root"), "vert", List.of());
        given(categoryService.getCategory(1, false)).willReturn(dto);

        mockMvc.perform(get("/api/v1/categories/{id}", 1).with(jwt()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1));
    }
}

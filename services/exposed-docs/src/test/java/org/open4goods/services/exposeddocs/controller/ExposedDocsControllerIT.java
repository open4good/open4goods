package org.open4goods.services.exposeddocs.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.open4goods.services.exposeddocs.ExposedDocsApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests covering exposed docs endpoints.
 */
@SpringBootTest(classes = ExposedDocsApplication.class)
@AutoConfigureMockMvc
public class ExposedDocsControllerIT
{

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies that the overview endpoint lists configured categories.
     *
     * @throws Exception when the request fails
     */
    @org.junit.jupiter.api.Test
    void shouldListCategories() throws Exception
    {
        mockMvc.perform(get("/exposed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].id").isNotEmpty());
    }

    /**
     * Verifies that category tree includes the sample README file.
     *
     * @throws Exception when the request fails
     */
    @org.junit.jupiter.api.Test
    void shouldExposeTree() throws Exception
    {
        mockMvc.perform(get("/exposed/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children[0].name").value("sample"));
    }

    /**
     * Verifies that content endpoint returns file contents.
     *
     * @throws Exception when the request fails
     */
    @org.junit.jupiter.api.Test
    void shouldReturnContent() throws Exception
    {
        mockMvc.perform(get("/exposed/docs/content")
                        .param("path", "sample/README.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Sample Documentation")));
    }

    /**
     * Verifies that search finds prompt resources when content is searched.
     *
     * @throws Exception when the request fails
     */
    @org.junit.jupiter.api.Test
    void shouldSearchPromptContent() throws Exception
    {
        mockMvc.perform(get("/exposed/search")
                        .param("query", "Summarize")
                        .param("categories", "prompts")
                        .param("searchContent", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("sample/summary.prompt"));
    }
}

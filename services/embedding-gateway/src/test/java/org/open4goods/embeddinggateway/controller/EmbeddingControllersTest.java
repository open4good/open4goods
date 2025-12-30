package org.open4goods.embeddinggateway.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.open4goods.embeddinggateway.config.EmbeddingGatewayProperties;
import org.open4goods.embeddinggateway.dto.ImageEmbeddingRequest;
import org.open4goods.embeddinggateway.dto.TextEmbeddingRequest;
import org.open4goods.embeddinggateway.service.ImageEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.open4goods.commons.services.TextEmbeddingService;
import org.mockito.Mockito;

@WebMvcTest({ TextEmbeddingController.class, ImageEmbeddingController.class })
@Import(EmbeddingControllersTest.TestConfig.class)
class EmbeddingControllersTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddingGatewayProperties properties;

    @Autowired
    private TextEmbeddingService textEmbeddingService;

    @Autowired
    private ImageEmbeddingService imageEmbeddingService;

    @Test
    void textEmbeddingReturnsVector() throws Exception
    {
        when(textEmbeddingService.embed("hello world")).thenReturn(new float[] { 0.1f, 0.2f });

        mockMvc.perform(post("/embeddings/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TextEmbeddingRequest("hello world"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedding[0]").value(0.1d))
                .andExpect(jsonPath("$.embedding[1]").value(0.2d));
    }

    @Test
    void imageEmbeddingReturnsVector() throws Exception
    {
        when(imageEmbeddingService.embed(ArgumentMatchers.any(URL.class))).thenReturn(new float[] { 0.4f, 0.5f });

        mockMvc.perform(post("/embeddings/image")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ImageEmbeddingRequest("https://example.com/image.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedding[0]").value(0.4d))
                .andExpect(jsonPath("$.embedding[1]").value(0.5d));
    }

    static class TestConfig
    {
        @Bean
        EmbeddingGatewayProperties properties()
        {
            return new EmbeddingGatewayProperties();
        }

        @Bean
        TextEmbeddingService textEmbeddingService()
        {
            return Mockito.mock(TextEmbeddingService.class);
        }

        @Bean
        ImageEmbeddingService imageEmbeddingService()
        {
            return Mockito.mock(ImageEmbeddingService.class);
        }
    }
}

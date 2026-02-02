package org.open4goods.services.geocode.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.open4goods.services.geocode.config.yml.GeoNamesProperties;
import org.open4goods.services.geocode.service.geonames.GeoNamesDatasetProvider;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the geocode endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GeocodeControllerIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Test
    void geocodeReturnsAlternateMatch() throws Exception
    {
        mockMvc.perform(get("/v1/geocode")
                .param("city", "Berlyn")
                .param("country", "DE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedName").value("Berlin"))
                .andExpect(jsonPath("$.matchType").value("ALTERNATE"));
    }

    @Test
    void geocodePrefersHigherPopulation() throws Exception
    {
        mockMvc.perform(get("/v1/geocode")
                .param("city", "Springfield")
                .param("country", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geonameId").value(4409896L));
    }

    @Test
    void distanceReturnsExpectedRange() throws Exception
    {
        mockMvc.perform(get("/v1/distance")
                .param("fromCity", "Paris")
                .param("fromCountry", "FR")
                .param("toCity", "Berlin")
                .param("toCountry", "DE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm", Matchers.greaterThan(850d)))
                .andExpect(jsonPath("$.distanceKm", Matchers.lessThan(900d)));
    }

    @TestConfiguration
    static class TestConfig
    {
        @Bean
        @Primary
        GeoNamesDatasetProvider geoNamesDatasetProvider() throws Exception
        {
            ClassPathResource resource = new ClassPathResource("geonames/cities5000_sample.txt");
            Path fixturePath = resource.getFile().toPath();
            RemoteFileCachingService stubCache = new RemoteFileCachingService("target/geocode-test-cache");
            GeoNamesProperties properties = new GeoNamesProperties();
            return new GeoNamesDatasetProvider(stubCache, properties)
            {
                @Override
                public Path getDatasetPath()
                {
                    return fixturePath;
                }
            };
        }
    }
}

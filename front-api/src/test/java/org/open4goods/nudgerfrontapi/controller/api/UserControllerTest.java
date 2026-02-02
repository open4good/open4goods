package org.open4goods.nudgerfrontapi.controller.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;
import org.open4goods.nudgerfrontapi.service.UserGeolocationService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link UserController}.
 */
class UserControllerTest
{
    private MockMvc mockMvc;
    private UserGeolocationService userGeolocationService;

    @BeforeEach
    void setUp()
    {
        userGeolocationService = ip -> new UserGeolocDto(
                ip,
                "Europe",
                "EU",
                "United Kingdom",
                "GB",
                "United Kingdom",
                "GB",
                "London",
                "England",
                "ENG",
                "EC1A",
                51.5142,
                -0.0931,
                5,
                "Europe/London",
                0,
                false,
                false);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userGeolocationService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldReturnGeolocation() throws Exception
    {
        mockMvc.perform(get("/user/geoloc")
                .param("domainLanguage", "fr")
                .param("ip", "81.2.69.142"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$.countryIsoCode").value("GB"))
                .andExpect(jsonPath("$.cityName").value("London"));
    }

    @Test
    void shouldReturnNotFoundWhenMissing() throws Exception
    {
        userGeolocationService = ip -> null;
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userGeolocationService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/user/geoloc")
                .param("domainLanguage", "fr")
                .param("ip", "203.0.113.1"))
                .andExpect(status().isNotFound());
    }
}

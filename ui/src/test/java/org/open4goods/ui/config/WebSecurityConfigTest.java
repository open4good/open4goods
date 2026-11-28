package org.open4goods.ui.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Access rules of {@link WebSecurityConfig} with {@code web-authentication: false}, the prod setting of
 * static.nudger.fr: public resources stay anonymous, while every actuator path (multi-segment ones included)
 * and the favicon cache purge need the actuator monitor identity.
 */
@SpringJUnitWebConfig(WebSecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
        "o4g.actuator.monitor.username=monitor",
        "o4g.actuator.monitor.password=monitor-secret"
})
class WebSecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicResourcesStayAnonymous() throws Exception {
        mockMvc.perform(get("/images/sample.webp")).andExpect(status().isOk());
    }

    @Test
    void multiSegmentActuatorPathsNeedTheMonitorIdentity() throws Exception {
        mockMvc.perform(post("/actuator/loggers/org.open4goods")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/actuator/loggers/org.open4goods").with(httpBasic("monitor", "monitor-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void singleSegmentActuatorPathsStillNeedTheMonitorIdentity() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health").with(httpBasic("monitor", "monitor-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void faviconCachePurgeNeedsTheMonitorIdentity() throws Exception {
        mockMvc.perform(delete("/favicon/cache")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/favicon/cache").with(httpBasic("monitor", "monitor-secret")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/favicon")).andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @Import({ WebSecurityConfig.class, ProbeController.class })
    static class TestConfig {

        @Bean
        UiConfig uiConfig() {
            UiConfig config = new UiConfig();
            config.getWebConfig().setWebAuthentication(false);
            return config;
        }
    }

    /** Stand-ins for the real endpoints: only the security rules in front of them are under test. */
    @RestController
    static class ProbeController {

        @GetMapping("/images/{name}")
        String image(@PathVariable String name) {
            return name;
        }

        @GetMapping({ "/actuator/health", "/actuator/metrics/{name}" })
        String actuatorRead() {
            return "ok";
        }

        @PostMapping("/actuator/loggers/{name}")
        String actuatorWrite(@PathVariable String name) {
            return name;
        }

        @GetMapping("/favicon")
        String favicon() {
            return "ok";
        }

        @DeleteMapping("/favicon/cache")
        String purgeFaviconCache() {
            return "purged";
        }
    }
}

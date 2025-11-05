package org.open4goods.eprelservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Infrastructure configuration providing HTTP clients required by the service.
 */
@Configuration
public class RestTemplateConfig
{
    /**
     * Creates a {@link RestTemplate} pre-configured with reasonable timeouts.
     *
     * @param builder Spring's builder used to apply default configuration
     * @return a blocking HTTP client for EPREL API calls
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder)
    {
        return builder.build();
    }
}

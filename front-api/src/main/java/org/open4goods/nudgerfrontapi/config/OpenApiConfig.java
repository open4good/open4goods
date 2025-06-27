package org.open4goods.nudgerfrontapi.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * SpringDoc configuration exposing only the frontend endpoints.
 */
public class OpenApiConfig {

    @Bean
    GroupedOpenApi frontApi() {
        return GroupedOpenApi.builder()
                .group("front")
                .pathsToMatch("/**")
                .build();
    }
}

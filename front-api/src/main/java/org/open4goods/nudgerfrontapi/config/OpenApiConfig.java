package org.open4goods.nudgerfrontapi.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

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

    @Bean
    OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        final String basicSchemeName = "basicAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .addSecurityItem(new SecurityRequirement().addList(basicSchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes(basicSchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic"))
                        );
    }
}

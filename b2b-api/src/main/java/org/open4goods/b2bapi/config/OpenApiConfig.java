package org.open4goods.b2bapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata and API-key security scheme.
 */
@Configuration
public class OpenApiConfig {

    public static final String PRODUCT_DATA_API_KEY = "productDataApiKey";

    @Bean
    public OpenAPI productDataOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Data API")
                        .version("v1")
                        .description("Metered B2B product-data API."))
                .components(new Components()
                        .addSecuritySchemes(PRODUCT_DATA_API_KEY, new SecurityScheme()
                                .name(PRODUCT_DATA_API_KEY)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("pdapi")));
    }
}

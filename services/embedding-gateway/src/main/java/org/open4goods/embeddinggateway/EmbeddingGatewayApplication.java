package org.open4goods.embeddinggateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot entry point for the embedding gateway.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EmbeddingGatewayApplication
{
    /**
     * Bootstraps the Spring context.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args)
    {
        SpringApplication.run(EmbeddingGatewayApplication.class, args);
    }
}

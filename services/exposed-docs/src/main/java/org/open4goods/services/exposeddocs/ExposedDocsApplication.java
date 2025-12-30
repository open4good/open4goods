package org.open4goods.services.exposeddocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot entry point for the exposed documentation microservice.
 * Boots the API that lists embedded documentation and prompt resources.
 */
@SpringBootApplication(scanBasePackages = "org.open4goods")
@ConfigurationPropertiesScan("org.open4goods")
public class ExposedDocsApplication
{

    /**
     * Starts the exposed docs microservice.
     *
     * @param args runtime arguments
     */
    public static void main(String[] args)
    {
        SpringApplication.run(ExposedDocsApplication.class, args);
    }
}

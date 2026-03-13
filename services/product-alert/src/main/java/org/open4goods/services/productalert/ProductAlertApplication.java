package org.open4goods.services.productalert;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot entry point for the product alert microservice.
 */
@SpringBootApplication(scanBasePackages = "org.open4goods")
@ConfigurationPropertiesScan("org.open4goods")
public class ProductAlertApplication
{
    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args)
    {
        org.springframework.boot.SpringApplication.run(ProductAlertApplication.class, args);
    }
}

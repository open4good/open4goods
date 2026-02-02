package org.open4goods.services.geocode;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot entry point for the geocode microservice.
 */
@SpringBootApplication(scanBasePackages = "org.open4goods")
@ConfigurationPropertiesScan("org.open4goods")
public class GeocodeApplication
{
    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args)
    {
        org.springframework.boot.SpringApplication.run(GeocodeApplication.class, args);
    }
}

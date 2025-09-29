package org.open4goods.nudgerfrontapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "org.open4goods")
@ConfigurationPropertiesScan("org.open4goods.nudgerfrontapi.config.properties")
@EnableCaching
/**
 * Spring Boot application entry point for the frontend API.
 */
public class NudgerFrontApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NudgerFrontApiApplication.class, args);
    }
}

package org.open4goods.nudgerfrontapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication(scanBasePackages = "org.open4goods")

@EnableCaching
/**
 * Spring Boot application entry point for the frontend API.
 */
public class NudgerFrontApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NudgerFrontApiApplication.class, args);
    }
}

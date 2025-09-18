package org.open4goods.b2b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication(scanBasePackages = "org.open4goods")
@OpenAPIDefinition(
        servers = {
            @Server(url = "http://localhost:8082", description = "Local development API"),
        }
)
@EnableCaching
/**
 * Spring Boot application entry point for the frontend API.
 */
public class B2bApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2bApplication.class, args);
    }
}

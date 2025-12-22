package org.open4goods.nudgerfrontapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "org.open4goods", exclude = {org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class})
@ConfigurationPropertiesScan("org.open4goods")
@EnableCaching
@EnableScheduling
/**
 * Spring Boot application entry point for the frontend API.
 */
public class NudgerFrontApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NudgerFrontApiApplication.class, args);
    }
}

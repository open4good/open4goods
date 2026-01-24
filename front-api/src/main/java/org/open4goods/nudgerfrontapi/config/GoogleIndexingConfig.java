package org.open4goods.nudgerfrontapi.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google Indexing support.
 */
@Configuration
public class GoogleIndexingConfig {

    /**
     * Provide a shared UTC clock for Google indexing components.
     *
     * @return UTC clock instance
     */
    @Bean
    public Clock googleIndexingClock() {
        return Clock.systemUTC();
    }
}

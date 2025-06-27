package org.open4goods.nudgerfrontapi.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;

/**
 * Cache related beans reused across controllers.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheControl oneHourPublicCache() {
        return CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
    }
}


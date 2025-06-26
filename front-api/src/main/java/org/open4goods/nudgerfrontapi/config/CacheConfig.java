package org.open4goods.nudgerfrontapi.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;

/**
 * Common cache configuration constants.
 */
@Configuration
public class CacheConfig {

    /**
     * Public cache for one hour.
     */
    public static final CacheControl ONE_HOUR_PUBLIC_CACHE =
            CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
}

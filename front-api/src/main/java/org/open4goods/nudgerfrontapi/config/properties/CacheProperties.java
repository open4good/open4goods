package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for front cache settings.
 */
@ConfigurationProperties(prefix = "front.cache")
public class CacheProperties {

    /** Path used to store cached files. */
    private String path = "./cache";

    /**
     * Time to live applied to the in-memory {@code ONE_HOUR_LOCAL_CACHE_NAME}
     * cache shared by search results and product references.
     */
    private Duration oneHourTtl = Duration.ofHours(1);

    /**
     * Maximum number of entries allowed in the {@code ONE_HOUR_LOCAL_CACHE_NAME}
     * cache before older entries are evicted.
     */
    private long oneHourMaximumSize = 5_000L;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Duration getOneHourTtl() {
        return oneHourTtl;
    }

    public void setOneHourTtl(Duration oneHourTtl) {
        this.oneHourTtl = oneHourTtl;
    }

    public long getOneHourMaximumSize() {
        return oneHourMaximumSize;
    }

    public void setOneHourMaximumSize(long oneHourMaximumSize) {
        this.oneHourMaximumSize = oneHourMaximumSize;
    }
}

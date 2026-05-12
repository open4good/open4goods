package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * Configuration properties for front cache settings.
 *
 * <p>Each cache is bounded by a byte budget (<code>*-max-bytes</code>) rather than
 * an entry count: a single fat response (e.g. a global search returning 100
 * enriched products) can be megabytes on its own, so capping by entry count is
 * not a memory-safety guarantee.</p>
 */
@ConfigurationProperties(prefix = "front.cache")
public class CacheProperties {

    /** Path used to store cached files. */
    private String path = "./cache";

    /** Expiry applied to the ONE_MINUTE_LOCAL_CACHE_NAME cache. */
    private Duration oneMinuteTtl = Duration.ofMinutes(1);

    /** Byte budget for the ONE_MINUTE_LOCAL_CACHE_NAME cache. */
    private DataSize oneMinuteMaxBytes = DataSize.ofMegabytes(32);

    /** Expiry applied to the ONE_HOUR_LOCAL_CACHE_NAME cache. */
    private Duration oneHourTtl = Duration.ofHours(1);

    /** Byte budget for the ONE_HOUR_LOCAL_CACHE_NAME cache. */
    private DataSize oneHourMaxBytes = DataSize.ofMegabytes(256);

    /** Expiry applied to the ONE_DAY_LOCAL_CACHE_NAME cache. */
    private Duration oneDayTtl = Duration.ofDays(1);

    /** Byte budget for the ONE_DAY_LOCAL_CACHE_NAME cache. */
    private DataSize oneDayMaxBytes = DataSize.ofMegabytes(256);

    /** Byte budget for the FOREVER_LOCAL_CACHE_NAME cache. */
    private DataSize foreverMaxBytes = DataSize.ofMegabytes(128);

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Duration getOneMinuteTtl() {
        return oneMinuteTtl;
    }

    public void setOneMinuteTtl(Duration oneMinuteTtl) {
        this.oneMinuteTtl = oneMinuteTtl;
    }

    public DataSize getOneMinuteMaxBytes() {
        return oneMinuteMaxBytes;
    }

    public void setOneMinuteMaxBytes(DataSize oneMinuteMaxBytes) {
        this.oneMinuteMaxBytes = oneMinuteMaxBytes;
    }

    public Duration getOneHourTtl() {
        return oneHourTtl;
    }

    public void setOneHourTtl(Duration oneHourTtl) {
        this.oneHourTtl = oneHourTtl;
    }

    public DataSize getOneHourMaxBytes() {
        return oneHourMaxBytes;
    }

    public void setOneHourMaxBytes(DataSize oneHourMaxBytes) {
        this.oneHourMaxBytes = oneHourMaxBytes;
    }

    public Duration getOneDayTtl() {
        return oneDayTtl;
    }

    public void setOneDayTtl(Duration oneDayTtl) {
        this.oneDayTtl = oneDayTtl;
    }

    public DataSize getOneDayMaxBytes() {
        return oneDayMaxBytes;
    }

    public void setOneDayMaxBytes(DataSize oneDayMaxBytes) {
        this.oneDayMaxBytes = oneDayMaxBytes;
    }

    public DataSize getForeverMaxBytes() {
        return foreverMaxBytes;
    }

    public void setForeverMaxBytes(DataSize foreverMaxBytes) {
        this.foreverMaxBytes = foreverMaxBytes;
    }
}

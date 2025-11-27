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

    /** Expiry applied to the ONE_MINUTE_LOCAL_CACHE_NAME cache. */
    private Duration oneMinuteTtl = Duration.ofMinutes(1);

    /** Maximum entries stored in the ONE_MINUTE_LOCAL_CACHE_NAME cache. */
    private long oneMinuteMaximumSize = 2_000L;

    /** Expiry applied to the ONE_HOUR_LOCAL_CACHE_NAME cache. */
    private Duration oneHourTtl = Duration.ofHours(1);

    /** Maximum entries stored in the ONE_HOUR_LOCAL_CACHE_NAME cache. */
    private long oneHourMaximumSize = 5_000L;

    /** Expiry applied to the ONE_DAY_LOCAL_CACHE_NAME cache. */
    private Duration oneDayTtl = Duration.ofDays(1);

    /** Maximum entries stored in the ONE_DAY_LOCAL_CACHE_NAME cache. */
    private long oneDayMaximumSize = 10_000L;

    /** Maximum entries stored in the FOREVER_LOCAL_CACHE_NAME cache. */
    private long foreverMaximumSize = 20_000L;



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

    public long getOneMinuteMaximumSize() {
        return oneMinuteMaximumSize;
    }

    public void setOneMinuteMaximumSize(long oneMinuteMaximumSize) {
        this.oneMinuteMaximumSize = oneMinuteMaximumSize;
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

    public Duration getOneDayTtl() {
        return oneDayTtl;
    }

    public void setOneDayTtl(Duration oneDayTtl) {
        this.oneDayTtl = oneDayTtl;
    }

    public long getOneDayMaximumSize() {
        return oneDayMaximumSize;
    }

    public void setOneDayMaximumSize(long oneDayMaximumSize) {
        this.oneDayMaximumSize = oneDayMaximumSize;
    }

    public long getForeverMaximumSize() {
        return foreverMaximumSize;
    }

    public void setForeverMaximumSize(long foreverMaximumSize) {
        this.foreverMaximumSize = foreverMaximumSize;
    }


}

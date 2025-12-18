package org.open4goods.nudgerfrontapi.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.open4goods.nudgerfrontapi.service.share.InMemoryShareResolutionStore;
import org.open4goods.nudgerfrontapi.service.share.ShareResolutionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for share resolution infrastructure components.
 */
@Configuration
public class ShareResolutionConfig {

    @Bean
    /**
     * Clock used to timestamp share resolutions.
     *
     * @return UTC system clock
     */
    public Clock shareResolutionClock() {
        return Clock.systemUTC();
    }

    @Bean
    /**
     * In-memory store keeping resolution snapshots with TTL.
     *
     * @param shareResolutionClock clock used for expiration
     * @return store implementation
     */
    public ShareResolutionStore shareResolutionStore(Clock shareResolutionClock) {
        return new InMemoryShareResolutionStore(shareResolutionClock);
    }

    @Bean
    /**
     * Dedicated executor used to process share resolutions asynchronously.
     *
     * @return configured executor
     */
    public Executor shareResolutionExecutor() {
        return Executors.newCachedThreadPool();
    }
}

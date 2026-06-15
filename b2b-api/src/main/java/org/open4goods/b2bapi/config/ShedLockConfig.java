package org.open4goods.b2bapi.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis-backed distributed scheduler lock configuration for clustered batches.
 */
@Configuration
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(final RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "b2b-api");
    }
}

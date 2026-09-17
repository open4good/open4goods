package org.open4goods.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables API scheduled jobs only when the shared scheduling switch permits them.
 * Administrative endpoints remain callable when scheduling is disabled.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "open4goods.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}

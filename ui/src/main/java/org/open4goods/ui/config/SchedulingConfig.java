package org.open4goods.ui.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/** Enables UI sitemap/open-data schedules only when the shared switch is enabled. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "open4goods.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig implements SchedulingConfigurer {

	/** Configures the dedicated scheduler used by UI maintenance jobs. */
	@Override
	public void configureTasks(
			ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskExecutor());
	}

    /** Returns the bounded scheduler executor. */
    @Bean(destroyMethod = "shutdown")
    Executor taskExecutor() {
		return Executors.newScheduledThreadPool(5);
	}
}

package org.open4goods.b2bapi;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Product Data API backend.
 */
@EnableCaching
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@SpringBootApplication(scanBasePackages = "org.open4goods")
@ConfigurationPropertiesScan("org.open4goods")
public class B2bApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(B2bApiApplication.class, args);
    }
}

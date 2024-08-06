package org.open4goods.ui;


import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.annotation.Timed;

@Component
public class MyServiceHealth implements HealthIndicator {

    @Override
    public Health health() {
        
        // code omitted that verifies service is working
        return Health.
        		up()
        		.withDetail("MyService", "Service is working")
        		.build();
    }
    
}
package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.services.googleindexation.service.GoogleIndexationService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Google Indexation pipeline in the front API.
 */
@Component
public class GoogleIndexationHealthIndicator implements HealthIndicator {

    private final GoogleIndexationProperties properties;
    private final GoogleIndexationQueueService queueService;
    private final GoogleIndexationService googleIndexationService;

    /**
     * Create the health indicator.
     *
     * @param properties indexation configuration
     * @param queueService queue service
     * @param googleIndexationService Google Indexing API client
     */
    public GoogleIndexationHealthIndicator(GoogleIndexationProperties properties,
            GoogleIndexationQueueService queueService,
            GoogleIndexationService googleIndexationService) {
        this.properties = properties;
        this.queueService = queueService;
        this.googleIndexationService = googleIndexationService;
    }

    /**
     * Report health for the queue and downstream client.
     *
     * @return health status
     */
    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("queueSize", queueService.getQueueSize())
                    .build();
        }
        Health serviceHealth = googleIndexationService.health();
        Health.Builder builder = serviceHealth.getStatus().equals(Status.UP)
                ? Health.up()
                : Health.down();
        builder.withDetail("enabled", true)
                .withDetail("queueSize", queueService.getQueueSize());
        if (queueService.getLastFailureMessage() != null) {
            builder.withDetail("lastQueueFailure", queueService.getLastFailureMessage());
        }
        return builder.build();
    }
}

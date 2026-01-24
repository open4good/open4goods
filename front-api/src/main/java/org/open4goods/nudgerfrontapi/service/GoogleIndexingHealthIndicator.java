package org.open4goods.nudgerfrontapi.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexingProperties;
import org.open4goods.nudgerfrontapi.service.dto.GoogleIndexingMetrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Google Indexing integration.
 */
@Component
public class GoogleIndexingHealthIndicator implements HealthIndicator {

    private final GoogleIndexingService googleIndexingService;
    private final GoogleIndexingProperties properties;
    private final Clock clock;

    /**
     * Create the health indicator.
     *
     * @param googleIndexingService service providing indexing metrics
     * @param properties            configuration properties
     * @param clock                 clock used to evaluate staleness
     */
    public GoogleIndexingHealthIndicator(GoogleIndexingService googleIndexingService,
                                         GoogleIndexingProperties properties,
                                         Clock clock) {
        this.googleIndexingService = googleIndexingService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Build a health snapshot for the indexing service.
     *
     * @return health status
     */
    @Override
    public Health health() {
        GoogleIndexingMetrics metrics = googleIndexingService.metrics();
        if (!metrics.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .build();
        }
        if (!googleIndexingService.isConfigured()) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("error", "Missing Google Indexing credentials")
                    .build();
        }
        Health.Builder builder = Health.up()
                .withDetail("enabled", true)
                .withDetail("pendingCount", metrics.pendingCount())
                .withDetail("deadLetterCount", metrics.deadLetterCount())
                .withDetail("lastSuccessAt", metrics.lastSuccessAt())
                .withDetail("lastFailureAt", metrics.lastFailureAt());

        if (metrics.pendingCount() > properties.getMaxQueueSize()) {
            return builder.down()
                    .withDetail("queueLimit", properties.getMaxQueueSize())
                    .build();
        }

        Instant now = clock.instant();
        Duration maxAge = properties.getMaxSuccessAge();
        if (metrics.pendingCount() > 0 && metrics.lastSuccessAt() != null) {
            Duration age = Duration.between(metrics.lastSuccessAt(), now);
            if (age.compareTo(maxAge) > 0) {
                return builder.down()
                        .withDetail("lastSuccessAge", age)
                        .withDetail("maxSuccessAge", maxAge)
                        .build();
            }
        }

        return builder.build();
    }
}

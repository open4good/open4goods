package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ProductViewService}.
 * <p>
 * Currently acts as a thin wrapper around the repository layer but keeps track
 * of critical exceptions for the health indicator.
 * </p>
 */
@Component
public class ProductViewServiceImpl implements ProductViewService, HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ProductViewServiceImpl.class);

    /** Counter for critical exceptions encountered when rendering. */
    private volatile int criticalExceptionCount;

    @Override
    public ProductViewResponse render(ProductViewRequest productViewRequest) {
        try {
            // TODO: fetch from repository and apply transformations
            return new ProductViewResponse(productViewRequest);
        } catch (Exception e) {
            logger.error("Critical error while rendering product view", e);
            criticalExceptionCount++;
            throw e;
        }
    }

    @Override
    public Health health() {
        if (criticalExceptionCount > 0) {
            return Health.down()
                    .withDetail("criticalExceptions", criticalExceptionCount)
                    .build();
        }
        return Health.up().build();
    }
}

package org.open4goods.services.productalert.health;

import org.open4goods.services.productalert.config.yml.ProductAlertProperties;
import org.open4goods.services.productalert.repository.ProductAlertNotificationCandidateRepository;
import org.open4goods.services.productalert.repository.ProductAlertSubscriptionRepository;
import org.open4goods.services.productalert.repository.ProductAlertUserRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator exposing repository availability and service settings.
 */
@Component
public class ProductAlertHealthIndicator implements HealthIndicator
{
    private final ProductAlertProperties properties;
    private final ProductAlertUserRepository userRepository;
    private final ProductAlertSubscriptionRepository subscriptionRepository;
    private final ProductAlertNotificationCandidateRepository notificationCandidateRepository;

    /**
     * Creates the health indicator.
     *
     * @param properties service properties
     * @param userRepository user repository
     * @param subscriptionRepository subscription repository
     * @param notificationCandidateRepository candidate repository
     */
    public ProductAlertHealthIndicator(ProductAlertProperties properties,
            ProductAlertUserRepository userRepository,
            ProductAlertSubscriptionRepository subscriptionRepository,
            ProductAlertNotificationCandidateRepository notificationCandidateRepository)
    {
        this.properties = properties;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationCandidateRepository = notificationCandidateRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Health health()
    {
        try
        {
            return Health.up()
                    .withDetail("securityEnabled", properties.getSecurity().isEnabled())
                    .withDetail("users", userRepository.count())
                    .withDetail("subscriptions", subscriptionRepository.count())
                    .withDetail("candidates", notificationCandidateRepository.count())
                    .build();
        }
        catch (Exception exception)
        {
            return Health.down(exception).build();
        }
    }
}

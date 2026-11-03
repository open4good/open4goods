package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;

import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Freshness rule applied to the offers exposed by front-api.
 * <p>
 * The api drops offers older than {@link ProductRepository#VALID_UNTIL_DURATION} when it re-aggregates a
 * product, but the daily batch only re-aggregates vertical products: products outside verticals keep their
 * old offers in Elasticsearch. front-api applies the same rule when it serves offers so that a stale price
 * or merchant link never reaches the frontend.
 */
@ConfigurationProperties(prefix = "front.offers")
public class OfferFreshnessProperties {

    /**
     * Maximum age of an offer, measured from its indexation timestamp. An older offer is not served.
     * Defaults to the api rule ({@link ProductRepository#VALID_UNTIL_DURATION}, 2 days).
     */
    private Duration maxAge = Duration.ofMillis(ProductRepository.VALID_UNTIL_DURATION);

    public Duration getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }
}

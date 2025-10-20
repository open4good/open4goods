package org.open4goods.nudgerfrontapi.config.properties;

import org.open4goods.model.priceevents.PriceRestitutionConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bind commercial event configuration to strongly typed objects shared with the
 * frontend.
 */
@ConfigurationProperties(prefix = "price-config")
public class PriceRestitutionProperties extends PriceRestitutionConfig {
}

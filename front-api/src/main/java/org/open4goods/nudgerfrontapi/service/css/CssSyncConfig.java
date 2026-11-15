package org.open4goods.nudgerfrontapi.service.css;

import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring of the CSS synchronisation (GOU-2081). Everything is conditional on {@code front.css.enabled=true} and
 * {@code front.css.sync.enabled=true}: with the default configuration no client, no job and no credential exist.
 */
@Configuration
@ConditionalOnProperty(prefix = "front.css", name = { "enabled", "sync.enabled" }, havingValue = "true")
public class CssSyncConfig {

    /**
     * @param properties CSS settings
     * @return the real Google client, unless {@code front.css.sync.simulated=true}
     */
    @Bean
    @ConditionalOnProperty(prefix = "front.css.sync", name = "simulated", havingValue = "false", matchIfMissing = true)
    CssProductsClient googleCssProductsClient(CssProperties properties) {
        return new GoogleCssProductsClient(properties.getSync());
    }

    /**
     * @return the client that never calls the network, when {@code front.css.sync.simulated=true}
     */
    @Bean
    @ConditionalOnProperty(prefix = "front.css.sync", name = "simulated", havingValue = "true")
    CssProductsClient simulatedCssProductsClient() {
        return new SimulatedCssProductsClient();
    }
}

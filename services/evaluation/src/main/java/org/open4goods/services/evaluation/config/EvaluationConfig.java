package org.open4goods.evaluation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the evaluation service.
 *
 * <p>This configuration allows setting properties related to Thymeleaf template caching.</p>
 *
 * <p>Configuration example in YAML:
 * <pre>
 * evaluation:
 *   template:
 *     cacheable: true
 * </pre>
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "evaluation.template")
public class EvaluationConfig {

    /**
     * Flag to enable or disable caching of Thymeleaf templates.
     */
    private boolean cacheable = true;

    public boolean isCacheable() {
        return cacheable;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }
}
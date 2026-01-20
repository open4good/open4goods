package org.open4goods.metriks.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Defines a remote SSH target.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerDefinition(
        String host,
        String user,
        Integer port
) {
}

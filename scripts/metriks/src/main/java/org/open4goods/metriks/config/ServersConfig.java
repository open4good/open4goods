package org.open4goods.metriks.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for SSH servers and mounts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServersConfig(
        Map<String, ServerDefinition> servers,
        List<String> mounts
) {
}

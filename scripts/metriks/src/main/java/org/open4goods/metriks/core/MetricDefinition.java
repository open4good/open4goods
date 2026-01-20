package org.open4goods.metriks.core;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Declarative definition of a metriks entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetricDefinition(
        String event_provider,
        String event_id,
        String eventName,
        String eventDescription,
        Map<String, String> params
) {
}

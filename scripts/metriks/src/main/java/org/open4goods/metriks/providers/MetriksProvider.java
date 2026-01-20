package org.open4goods.metriks.providers;

import java.util.Optional;

import org.open4goods.metriks.core.MetricDefinition;
import org.open4goods.metriks.core.MetrikPayload;
import org.open4goods.metriks.core.Period;

/**
 * Provider able to generate metriks payloads for a given definition.
 */
public interface MetriksProvider {

    /**
     * Collect data for the provided definition.
     *
     * @param definition metric definition
     * @param period period information
     * @return payload or empty if not supported
     */
    Optional<MetrikPayload> collect(MetricDefinition definition, Period period);
}

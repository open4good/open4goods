package org.open4goods.metriks.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard metriks payload persisted in the history folder.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetrikPayload(
        String schemaVersion,
        String event_id,
        String event_provider,
        String eventName,
        String eventDescription,
        PeriodPayload period,
        Instant collectedAt,
        String status,
        String errorMessage,
        EventData eventData,
        EventVariation eventVariation,
        String eventUrl
) {

    /**
     * Period metadata stored inside the payload.
     */
    public record PeriodPayload(
            int lastPeriodInDays,
            String startDate,
            String endDate
    ) {
    }

    /**
     * Value payload for a metriks entry.
     */
    public record EventData(
            BigDecimal value,
            String unit,
            Map<String, BigDecimal> breakdown
    ) {
    }

    /**
     * Optional variation data when computed at the provider level.
     */
    public record EventVariation(
            BigDecimal absolute,
            BigDecimal percent
    ) {
    }
}

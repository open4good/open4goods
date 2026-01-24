package org.open4goods.nudgerfrontapi.dto.metriks;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cell value for a metriks row.
 *
 * @param dateKey date key (YYYYMMDD)
 * @param value metric value
 * @param variationAbs absolute variation against the previous period
 * @param variationPct percentage variation against the previous period
 * @param payload optional raw payload
 */
public record MetriksReportValueDto(
        @Schema(description = "Date key", example = "20250108")
        String dateKey,
        @Schema(description = "Metric value", nullable = true, example = "42")
        BigDecimal value,
        @Schema(description = "Absolute variation", nullable = true, example = "5")
        BigDecimal variationAbs,
        @Schema(description = "Percentage variation", nullable = true, example = "12.5")
        BigDecimal variationPct,
        @Schema(description = "Raw metriks payload", nullable = true, type = "object")
        JsonNode payload
) {
}

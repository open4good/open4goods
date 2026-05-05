package org.open4goods.nudgerfrontapi.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable profile metrics used by profile dashboard and detail pages.
 *
 * @param nodesConfiguredCount count of configured agents for the authenticated user
 * @param apiKeysCount count of API keys owned by the user
 * @param organizationsCount count of organizations linked to the user
 * @param billedMonthTokens billed tokens for the current month
 * @param billedLifetimeTokens billed tokens across all time
 * @param paidMonthTokens paid tokens for the current month
 * @param paidLifetimeTokens paid tokens across all time
 * @param paidMonthAmount paid monetary amount for the current month
 * @param paidLifetimeAmount paid monetary amount across all time
 * @param currency ISO-4217 currency code for amount fields
 */
@Schema(description = "Profile KPI metrics.")
public record ProfileMetricsDto(
        @Schema(description = "Configured agent count.", example = "3")
        int nodesConfiguredCount,
        @Schema(description = "API key count.", example = "2")
        int apiKeysCount,
        @Schema(description = "Linked organizations count.", example = "1")
        int organizationsCount,
        @Schema(description = "Billed token volume for current month.", example = "120345")
        long billedMonthTokens,
        @Schema(description = "Billed token volume for lifetime.", example = "987654")
        long billedLifetimeTokens,
        @Schema(description = "Paid token volume for current month.", example = "100000")
        long paidMonthTokens,
        @Schema(description = "Paid token volume for lifetime.", example = "910000")
        long paidLifetimeTokens,
        @Schema(description = "Paid amount for current month.", example = "39.90")
        double paidMonthAmount,
        @Schema(description = "Paid amount for lifetime.", example = "462.15")
        double paidLifetimeAmount,
        @Schema(description = "Currency used by amount fields.", example = "EUR")
        String currency
) {}

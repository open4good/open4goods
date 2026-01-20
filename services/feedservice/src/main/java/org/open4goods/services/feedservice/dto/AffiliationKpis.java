package org.open4goods.services.feedservice.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated KPI values for affiliation providers.
 *
 * @param clicks number of clicks
 * @param impressions number of impressions
 * @param transactionsTotal total number of transactions
 * @param transactionsConfirmed number of confirmed transactions
 * @param transactionsPending number of pending transactions
 * @param commissionTotal total commissions amount
 * @param turnoverTotal total turnover amount
 * @param breakdown optional breakdown per program
 */
public record AffiliationKpis(
        long clicks,
        long impressions,
        long transactionsTotal,
        long transactionsConfirmed,
        long transactionsPending,
        BigDecimal commissionTotal,
        BigDecimal turnoverTotal,
        Map<String, AffiliationKpisBreakdown> breakdown
) {

    /**
     * Aggregated values for a single program within the provider breakdown.
     */
    public record AffiliationKpisBreakdown(
            long clicks,
            long impressions,
            long transactionsTotal,
            long transactionsConfirmed,
            long transactionsPending,
            BigDecimal commissionTotal,
            BigDecimal turnoverTotal
    ) {
    }
}

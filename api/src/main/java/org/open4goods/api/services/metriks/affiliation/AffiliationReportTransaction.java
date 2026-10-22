package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One sale reported by an affiliation network, normalised for the Metriks affiliation report.
 *
 * @param transactionDate date of the transaction (the sale), in the report's time zone (Europe/Paris)
 * @param status          normalised status
 * @param commission      publisher commission; may be {@code null} only for a {@link AffiliationReportStatus#DECLINED}
 *                        transaction, which is never summed
 * @param currency        ISO 4217 currency code of {@code commission}, as reported by the network
 */
public record AffiliationReportTransaction(LocalDate transactionDate, AffiliationReportStatus status,
                                           BigDecimal commission, String currency) {
}

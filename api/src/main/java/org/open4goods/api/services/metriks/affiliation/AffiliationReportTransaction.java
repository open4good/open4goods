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
 * @param advertiserId    network identifier of the advertiser (programme), {@code null} when the network does not
 *                        provide it: the sale is then attributed to no merchant
 * @param advertiserName  advertiser name as known by the network, {@code null} when not provided
 * @param clickRef        click reference echoed by the network ({@code clickRef} of the tracking link), {@code null}
 *                        when absent; never aggregated here, read for the sale-to-click matching
 */
public record AffiliationReportTransaction(LocalDate transactionDate, AffiliationReportStatus status,
                                           BigDecimal commission, String currency, String advertiserId,
                                           String advertiserName, String clickRef) {

    /**
     * Transaction without advertiser nor click reference.
     */
    public AffiliationReportTransaction(LocalDate transactionDate, AffiliationReportStatus status,
                                        BigDecimal commission, String currency) {
        this(transactionDate, status, commission, currency, null, null, null);
    }
}

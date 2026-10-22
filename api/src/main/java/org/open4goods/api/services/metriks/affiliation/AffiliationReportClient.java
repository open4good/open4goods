package org.open4goods.api.services.metriks.affiliation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Reads the transactions of one affiliation network for the Metriks affiliation report
 * ({@code GET /api/metriks/affiliation}).
 * <p>
 * Unlike the lenient {@code AffiliationProvider#getTransactions} SPI of the feed service, which returns an
 * empty list on error, an implementation fails with an {@link AffiliationReportException}: Metriks must report
 * an error, never a zero, when a network cannot be read.
 */
public interface AffiliationReportClient {

    /**
     * @return network key used in the Metriks event ids and properties ({@code awin}, {@code effiliation},
     *         {@code rakuten})
     */
    String network();

    /**
     * @return network name used in error messages
     */
    String displayName();

    /**
     * @return full names of the required {@code metriks.affiliation.<network>.*} properties that are not set,
     *         in declaration order; empty when the client is configured
     */
    List<String> missingCredentials();

    /**
     * @return the configured credential values, so that callers can scrub them from any message as a last
     *         line of defence; blank values are omitted
     */
    List<String> secretValues();

    /**
     * Fetches the transactions whose transaction (sale) date lies in {@code [startDate, endDate]}.
     *
     * @param startDate first day of the window, inclusive
     * @param endDate   last day of the window, inclusive
     * @param zone      time zone in which the window days, and the returned transaction dates, are expressed
     * @return normalised transactions, declined ones included
     * @throws AffiliationReportException on HTTP error, timeout, unreadable response or unknown status
     */
    List<AffiliationReportTransaction> fetchTransactions(LocalDate startDate, LocalDate endDate, ZoneId zone)
            throws AffiliationReportException;
}

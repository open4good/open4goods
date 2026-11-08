package org.open4goods.api.services.metriks.affiliation;

/**
 * Status of an affiliation transaction, normalised across networks for the Metriks affiliation report.
 * <p>
 * Each {@link AffiliationReportClient} documents, in its Javadoc, how its network's statuses map onto
 * these three values. An unknown network status is a parsing error, never a silent default.
 */
public enum AffiliationReportStatus {

    /** Confirmed by the advertiser: the commission is due. */
    VALIDATED,

    /** Recorded but not yet confirmed by the advertiser. */
    PENDING,

    /** Refused, cancelled or deleted: excluded from every Metriks sum and count. */
    DECLINED
}

package org.open4goods.datareference.model;

import java.net.URI;
import java.util.Objects;

/**
 * Attachment of one provider record to one GTIN, with the evidence supporting it.
 *
 * <p>A record may legitimately link to several GTINs, and a link may later be
 * withdrawn or re-evidenced without touching {@link SourceRecordKey}. Links are
 * ordered on the head so that the resolver has a deterministic preference when
 * two links carry the same confidence.
 *
 * @param gtin product identity the record is attached to
 * @param confidence strength of the attachment
 * @param method how the attachment was established
 * @param evidenceReference durable pointer to the supporting evidence, or {@code null}
 */
public record GtinLink(
        Gtin gtin,
        GtinMatchConfidence confidence,
        GtinMatchMethod method,
        URI evidenceReference) {

    /**
     * Validates the attachment.
     */
    public GtinLink {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(method, "method must not be null");
    }
}

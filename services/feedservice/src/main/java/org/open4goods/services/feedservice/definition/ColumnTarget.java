package org.open4goods.services.feedservice.definition;

import java.util.Objects;

import org.open4goods.datareference.model.SourceContentType;

/**
 * What a source column feeds: a reference assertion, or an offer field.
 *
 * <p>The two are mutually exclusive by construction. {@link ReferenceField}
 * carries a {@link SourceContentType} other than {@code OFFER}/{@code PRICE},
 * because those two content types describe offer state for usage-policy
 * grants, not a shape a reference assertion may take. Price, availability and
 * offer condition columns are always {@link OfferField}, which routes to
 * price-history's {@code OfferHead} ingestion and never produces a reference
 * assertion.
 */
public sealed interface ColumnTarget {

    /**
     * A column that becomes a reference assertion.
     *
     * @param contentType kind of content, for usage-policy filtering
     * @param canonicalFieldId canonical attribute or identity field this column feeds
     */
    record ReferenceField(SourceContentType contentType, String canonicalFieldId) implements ColumnTarget {
        public ReferenceField {
            Objects.requireNonNull(contentType, "contentType must not be null");
            if (contentType == SourceContentType.OFFER || contentType == SourceContentType.PRICE) {
                throw new IllegalArgumentException(
                        "reference field mapping must not use OFFER or PRICE content type; "
                                + "use ColumnTarget.OfferField instead");
            }
            canonicalFieldId = requireText(canonicalFieldId, "canonicalFieldId");
        }
    }

    /**
     * A column that feeds price-history offer ingestion, never a reference assertion.
     *
     * @param field which offer field this column carries
     */
    record OfferField(OfferFieldKind field) implements ColumnTarget {
        public OfferField {
            Objects.requireNonNull(field, "field must not be null");
        }
    }

    /** Offer-only fields; never reference assertions (AC3). */
    enum OfferFieldKind {
        PRICE,
        AVAILABILITY,
        OFFER_CONDITION
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

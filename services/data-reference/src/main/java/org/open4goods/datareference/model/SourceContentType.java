package org.open4goods.datareference.model;

/**
 * Kind of content an assertion carries, as usage policies see it.
 *
 * <p>This is the axis a licence reasons about: a source may permit republishing
 * a classification while forbidding republishing its texts or images. It is
 * independent of the evidence shape, which describes how a value is structured.
 */
public enum SourceContentType {
    /** GTIN, brand, model and other identity fields. */
    IDENTITY,
    /** Category, class or taxonomy membership. */
    CLASSIFICATION,
    /** Measured or declared product characteristics. */
    ATTRIBUTE,
    /** Names, descriptions, marketing copy and other prose. */
    TEXT,
    /** Images, documents and other binary content. */
    MEDIA,
    /** Links to other products, models or families. */
    RELATION,
    /** Merchant offer terms. */
    OFFER,
    /** Observed prices. */
    PRICE
}

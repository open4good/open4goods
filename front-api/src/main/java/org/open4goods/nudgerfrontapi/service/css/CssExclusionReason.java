package org.open4goods.nudgerfrontapi.service.css;

/**
 * Why a product is not eligible to the Google CSS API. One reason per failed rule (GOU-2080).
 */
public enum CssExclusionReason {

    /** No fresh offer with a price and a currency. */
    NO_FRESH_OFFER,

    /** Every fresh offer comes from affiliation networks, which are not merchants. */
    AFFILIATION_NETWORK_ONLY,

    /** The merchant domain of no fresh offer can be resolved. */
    UNRESOLVED_MERCHANT_DOMAIN,

    /** Fewer distinct merchant domains than required (for instance {@code fr.aliexpress.com} + {@code aliexpress.com}). */
    NOT_ENOUGH_MERCHANT_DOMAINS,

    /** No image. */
    NO_IMAGE,

    /** No title, the GTIN alone or a generic title. */
    NO_REAL_TITLE,

    /** The GTIN is missing, not numeric or its check digit is wrong. */
    INVALID_GTIN,

    /** The canonical URL cannot be computed (no slug). */
    NO_CANONICAL_URL
}

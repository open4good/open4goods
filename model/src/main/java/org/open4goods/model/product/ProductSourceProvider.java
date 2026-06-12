package org.open4goods.model.product;

/**
 * Provider that discovered or created a product source URL.
 */
public enum ProductSourceProvider {
    DATAFORSEO,
    GOOGLE_CUSTOM_SEARCH,
    OFFICIAL_DOMAIN,
    STRUCTURED_FACTS,
    LEGACY_REVIEW_FACT,
    MANUAL,
    UNKNOWN
}

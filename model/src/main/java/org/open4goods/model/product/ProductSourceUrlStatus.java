package org.open4goods.model.product;

/**
 * Processing state for a source URL in the product enrichment pipeline.
 */
public enum ProductSourceUrlStatus {
    DISCOVERED,
    IDENTIFIED,
    FETCHING,
    FETCHED,
    REJECTED,
    FAILED
}

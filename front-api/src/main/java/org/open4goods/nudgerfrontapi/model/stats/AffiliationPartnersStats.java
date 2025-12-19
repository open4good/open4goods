package org.open4goods.nudgerfrontapi.model.stats;

/**
 * Immutable snapshot of affiliation partner statistics exposed through the statistics service.
 * <p>
 * Keeping partner-related computations encapsulated simplifies extension when richer metrics
 * (such as product counts per partner) become available.
 * </p>
 *
 * @param count total number of partners available for affiliation.
 */
public record AffiliationPartnersStats(int count) { }

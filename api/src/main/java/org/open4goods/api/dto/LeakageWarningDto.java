package org.open4goods.api.dto;

/**
 * Cross-vertical category distribution for a datasourceCategories value.
 *
 * @param category exact datasourceCategories value
 * @param totalDocs total products carrying the category and a vertical
 * @param topVertical highest-volume vertical for this category
 * @param topShare share of totalDocs for the highest-volume vertical
 * @param secondVertical runner-up vertical, or {@code null}
 * @param secondShare share of totalDocs for the runner-up vertical
 * @param flagged true when the runner-up share is at or above the requested threshold
 */
public record LeakageWarningDto(
        String category,
        long totalDocs,
        String topVertical,
        double topShare,
        String secondVertical,
        double secondShare,
        boolean flagged
) {}

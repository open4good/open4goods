package org.open4goods.nudgerfrontapi.dto;

/**
 * Metadata describing a request lifecycle.
 */
public record RequestMetadata(Long startDate, Long endDate, String fishtag) {
}

package org.open4goods.nudgerfrontapi.dto;

/**
 * Incoming vote payload.
 */
public record VoteRequest(String itemId, int value) {
}

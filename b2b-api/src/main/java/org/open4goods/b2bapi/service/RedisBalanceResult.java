package org.open4goods.b2bapi.service;

/**
 * Redis balance script result.
 *
 * @param status outcome status
 * @param balance resulting balance when available
 */
public record RedisBalanceResult(
        RedisBalanceStatus status,
        long balance) {
}

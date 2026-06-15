package org.open4goods.b2bapi.service;

/**
 * Outcome of a Redis balance script invocation.
 */
public enum RedisBalanceStatus {

    RESERVED,
    UPDATED,
    INSUFFICIENT_CREDITS,
    BALANCE_NOT_LOADED
}

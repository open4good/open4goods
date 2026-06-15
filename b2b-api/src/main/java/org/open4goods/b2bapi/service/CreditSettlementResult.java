package org.open4goods.b2bapi.service;

/**
 * Result of a durable credit settlement.
 *
 * @param durableBalance authoritative live balance after settlement
 * @param creditsDebited credits debited by this invocation
 * @param idempotentReplay whether a prior debit for the request already existed
 */
public record CreditSettlementResult(
        long durableBalance,
        long creditsDebited,
        boolean idempotentReplay) {
}

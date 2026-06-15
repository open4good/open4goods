package org.open4goods.b2bapi.service;

import java.util.UUID;

/**
 * Result of a durable credit grant or grant-related ledger operation.
 *
 * @param bucketId created bucket id, when a bucket was created
 * @param creditsGranted positive credits granted by this operation
 * @param creditsExpired credits expired while enforcing caps
 * @param durableBalance authoritative live balance after the operation
 * @param idempotentReplay whether a one-time grant was already applied
 */
public record CreditGrantResult(
        UUID bucketId,
        long creditsGranted,
        long creditsExpired,
        long durableBalance,
        boolean idempotentReplay) {
}

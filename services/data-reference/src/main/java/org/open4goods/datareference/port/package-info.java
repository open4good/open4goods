/**
 * Typed ports the source-neutral product reference offers to its callers.
 *
 * <p>Every port is an interface owned by this module and implemented outside it.
 * {@code api} and the provider adapters depend on these types; nothing here
 * depends on them. That direction is what lets a provider be added, or the
 * legacy aggregator retired, without touching the contract.
 *
 * <p>Batch ports state their cursor, their ordering and what they do when one
 * element fails, because a scan over millions of records that silently reorders
 * or silently drops is indistinguishable from one that works.
 */
package org.open4goods.datareference.port;

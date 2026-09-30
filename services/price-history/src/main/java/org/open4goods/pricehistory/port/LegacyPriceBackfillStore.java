package org.open4goods.pricehistory.port;

import org.open4goods.pricehistory.model.LegacyPriceBackfill;

/** Idempotent storage of neutral legacy Product minimum-price backfills. */
public interface LegacyPriceBackfillStore {

    /** Appends a one-time legacy point by deterministic identifier. */
    boolean append(LegacyPriceBackfill backfill);
}

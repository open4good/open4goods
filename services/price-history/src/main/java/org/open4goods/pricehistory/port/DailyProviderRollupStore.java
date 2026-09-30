package org.open4goods.pricehistory.port;

import java.time.LocalDate;
import java.util.Optional;

import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.DailyRollupKey;

/** Durable query store for daily provider rollups. */
public interface DailyProviderRollupStore {

    /** Reads one materialized daily bucket. */
    Optional<DailyProviderRollup> find(DailyRollupKey key, LocalDate day);

    /** Replaces an open or explicitly rebuilt bucket atomically. */
    void upsert(DailyProviderRollup rollup);
}

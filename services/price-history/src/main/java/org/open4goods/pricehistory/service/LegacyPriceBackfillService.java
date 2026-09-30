package org.open4goods.pricehistory.service;

import java.util.Objects;

import org.open4goods.pricehistory.model.LegacyMinimumPricePoint;
import org.open4goods.pricehistory.model.LegacyPriceBackfill;
import org.open4goods.pricehistory.port.LegacyPriceBackfillStore;

/** Imports legacy Product minima once without fabricating an offer or provider observation. */
public final class LegacyPriceBackfillService {

    private final LegacyPriceBackfillStore store;

    /** Creates the deterministic legacy importer. */
    public LegacyPriceBackfillService(LegacyPriceBackfillStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    /**
     * Persists the legacy point's separate neutral shape.
     *
     * @param point original legacy minimum data
     * @return whether this invocation stored the point for the first time
     */
    public boolean importOnce(LegacyMinimumPricePoint point) {
        return store.append(LegacyPriceBackfill.of(point));
    }
}

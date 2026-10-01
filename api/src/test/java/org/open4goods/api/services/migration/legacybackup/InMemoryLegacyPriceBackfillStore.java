package org.open4goods.api.services.migration.legacybackup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import org.open4goods.pricehistory.model.LegacyPriceBackfill;
import org.open4goods.pricehistory.port.LegacyPriceBackfillStore;

/** Minimal in-memory {@link LegacyPriceBackfillStore} test double. */
final class InMemoryLegacyPriceBackfillStore implements LegacyPriceBackfillStore {

    private final Set<String> ids = ConcurrentHashMap.newKeySet();

    @Override
    public boolean append(LegacyPriceBackfill backfill) {
        return ids.add(backfill.id());
    }

    int size() {
        return ids.size();
    }
}

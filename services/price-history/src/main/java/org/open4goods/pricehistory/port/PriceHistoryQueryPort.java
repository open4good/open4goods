package org.open4goods.pricehistory.port;

import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.LegacyPriceBackfill;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceHistoryPage;
import org.open4goods.pricehistory.model.PriceHistoryQuery;

/**
 * Provider-price query port. Implementations preserve ascending order and opaque cursor semantics.
 * The {@link PriceHistoryQuery} contract supplies inclusive/exclusive UTC filtering and chooses
 * daily rollups for long requests unless callers explicitly select a representation.
 */
public interface PriceHistoryQueryPort {

    /** Queries sparse raw change events. */
    PriceHistoryPage<PriceChangeEvent> queryChanges(PriceHistoryQuery query);

    /** Queries daily provider rollups. */
    PriceHistoryPage<DailyProviderRollup> queryDaily(PriceHistoryQuery query);

    /** Queries separately shaped legacy minima only when explicitly requested. */
    PriceHistoryPage<LegacyPriceBackfill> queryLegacy(PriceHistoryQuery query);
}

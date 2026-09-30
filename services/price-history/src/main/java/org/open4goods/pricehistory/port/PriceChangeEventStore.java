package org.open4goods.pricehistory.port;

import org.open4goods.pricehistory.model.PriceChangeEvent;

/** Append-only delivery of sparse provider price changes. */
public interface PriceChangeEventStore {

    /**
     * Appends an event idempotently.
     *
     * @param event immutable price transition
     * @return whether a new event was stored
     */
    boolean append(PriceChangeEvent event);
}

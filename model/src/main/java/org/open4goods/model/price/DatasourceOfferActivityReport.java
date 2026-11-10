package org.open4goods.model.price;

import java.util.List;

/**
 * Snapshot of the last offer date of every datasource, served by the api on {@code GET /merchants/offer-activity}
 * and read by front-api to count the merchants that have an active offer (GOU-2037).
 *
 * @param trackedSince epoch milliseconds since which the api records offer dates: a datasource whose last offer is
 *                     older than this date may be missing from {@code datasources}
 * @param datasources  last offer date per datasource, sorted by datasource name
 */
public record DatasourceOfferActivityReport(long trackedSince, List<DatasourceOfferActivity> datasources) {

    /**
     * Normalises a missing list to an empty one.
     */
    public DatasourceOfferActivityReport {
        datasources = datasources == null ? List.of() : List.copyOf(datasources);
    }
}

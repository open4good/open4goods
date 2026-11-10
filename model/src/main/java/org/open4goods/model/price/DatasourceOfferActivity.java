package org.open4goods.model.price;

/**
 * Date of the last offer a datasource brought to a product (GOU-2037).
 * <p>
 * Recorded by the api each time a data fragment carrying an offer (non-zero price, not out of stock) is aggregated
 * into a product, so that front-api can tell which merchants still have an active offer without scanning the
 * product index, where {@code price.offers} is not indexed.
 * </p>
 *
 * @param datasourceName datasource name, as in {@link AggregatedPrice#getDatasourceName()}
 * @param lastOfferSeen  epoch milliseconds of the most recent offer timestamp of this datasource
 */
public record DatasourceOfferActivity(String datasourceName, long lastOfferSeen) {
}

package org.open4goods.api.services;

/**
 * What a datasource purge removes from the product index (GOU-2101).
 */
public enum DatasourcePurgeScope {

	/**
	 * Only the offers of the datasource ({@code price.offers}), then the recomputed best offer, offers count and
	 * conditions. The product sheet data of the datasource (attribute sources, descriptions, categories, brand
	 * aliases, {@code datasourceCodes}) stays. Written by partial updates of {@code price} and {@code offersCount}.
	 * The default: the least destructive scope.
	 */
	OFFERS,

	/**
	 * The offers and every other piece of data of the datasource ({@code Product#removeDatasourceData}). Written by
	 * storing the complete product.
	 */
	ALL
}

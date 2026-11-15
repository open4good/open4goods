package org.open4goods.api.services.backfill;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * One kind of batched update of the product index run by {@link BackfillEngine} (GOU-2266): which products to read,
 * which fields of their source, and the change of each one. An operation must be idempotent: a product already
 * changed gets no new change, so a run started again from the beginning only rewrites what is left.
 */
public interface BackfillOperation {

	/**
	 * @return short name used in logs, in the status and in the backup file names, for instance {@code decitre-purge}
	 */
	String name();

	/**
	 * @return the products to scan
	 */
	Query query();

	/**
	 * @return the source fields to read, never empty: the scan never loads complete products
	 */
	List<String> sourceFields();

	/**
	 * Plans the change of one product.
	 *
	 * @param id     product id
	 * @param source the requested source fields of the product, as stored in the index
	 * @return the change to apply, empty when the product needs none
	 */
	Optional<BackfillChange> plan(String id, Map<String, Object> source);

	/**
	 * @return painless script putting back the {@link BackfillChange#backup()} of a product, given as
	 *         {@code params.data}; null when the operation cannot be rolled back
	 */
	String restoreScript();
}

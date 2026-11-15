package org.open4goods.api.services.backfill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.product.BarcodeType;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Removes attribute keys from {@code attributes.all} of the indexed books (GOU-2266, decision of 2026-11-15 in
 * GOU-2262: no Decitre classification anywhere on nudger.fr).
 *
 * <p>Scans the products whose {@code gtinInfos.upcType} is {@code ISBN_13}: {@code attributes.all} is an
 * {@code enabled:false} object, so its keys can be neither queried nor counted, and the dry run of this operation is
 * the count. Only the targeted keys are read ({@code attributes.all.<key>} source filtering), and only those present
 * are removed, by a painless script: no other field is written, {@code lastChange} included. Each removed attribute is
 * saved whole in the backup, and {@link #restoreScript()} puts it back.
 */
public class DecitrePurgeOperation implements BackfillOperation {

	/** Name of the operation, in the endpoint, the status and the backup file names. */
	public static final String NAME = "decitre-purge";

	/**
	 * The keys excluded at ingestion by GOU-2264 ({@code attributesConfig.exclusions} of {@code _default.yml}): the
	 * three Decitre classifications, the raw {@code ATTRIBUTS} blob that copies them, and the two keys whose value is
	 * Decitre.
	 */
	public static final List<String> DEFAULT_KEYS = List.of("CLASSIFICATION DECITRE 1", "CLASSIFICATION DECITRE 2",
			"CLASSIFICATION DECITRE 3", "ATTRIBUTS", "LIVRES ORIGINE", "PROPRIETAIRE", "PROPRIÉTAIRE");

	/** Removes the planned keys still present; a product that no longer holds any is left untouched (noop). */
	static final String PURGE_SCRIPT = "if (ctx._source.attributes == null || ctx._source.attributes.all == null) { ctx.op = 'noop'; } "
			+ "else { boolean changed = false; "
			+ "for (def key : params.keys) { if (ctx._source.attributes.all.containsKey(key)) { ctx._source.attributes.all.remove(key); changed = true; } } "
			+ "if (!changed) { ctx.op = 'noop'; } }";

	/** Puts back the saved attributes, creating {@code attributes.all} when needed. */
	static final String RESTORE_SCRIPT = "if (ctx._source.attributes == null) { ctx._source.attributes = [:]; } "
			+ "if (ctx._source.attributes.all == null) { ctx._source.attributes.all = [:]; } "
			+ "for (def entry : params.data.entrySet()) { ctx._source.attributes.all[entry.getKey()] = entry.getValue(); }";

	private final List<String> keys;

	/** Purge of {@link #DEFAULT_KEYS}. */
	public DecitrePurgeOperation() {
		this(DEFAULT_KEYS);
	}

	/**
	 * @param keys exact keys of {@code attributes.all} to remove; blank keys are ignored
	 * @throws IllegalArgumentException when no key is left, or a key contains a dot (it would be read as a path)
	 */
	public DecitrePurgeOperation(List<String> keys) {
		List<String> cleaned = keys == null ? List.of()
				: keys.stream().filter(StringUtils::isNotBlank).distinct().toList();
		if (cleaned.isEmpty()) {
			throw new IllegalArgumentException("at least one key is required");
		}
		if (cleaned.stream().anyMatch(key -> key.contains("."))) {
			throw new IllegalArgumentException("a key cannot contain a dot");
		}
		this.keys = cleaned;
	}

	/** @return the keys removed by this operation */
	public List<String> keys() {
		return keys;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public Query query() {
		return Query.of(q -> q.term(t -> t.field("gtinInfos.upcType").value(BarcodeType.ISBN_13.name())));
	}

	@Override
	public List<String> sourceFields() {
		return keys.stream().map(key -> "attributes.all." + key).toList();
	}

	@Override
	public Optional<BackfillChange> plan(String id, Map<String, Object> source) {
		if (!(source.get("attributes") instanceof Map<?, ?> attributes) || !(attributes.get("all") instanceof Map<?, ?> all)) {
			return Optional.empty();
		}
		Map<String, Object> removed = new LinkedHashMap<>();
		List<String> present = new ArrayList<>();
		for (String key : keys) {
			if (all.containsKey(key)) {
				removed.put(key, all.get(key));
				present.add(key);
			}
		}
		if (present.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new BackfillChange(PURGE_SCRIPT, Map.of("keys", List.copyOf(present)), removed, present));
	}

	@Override
	public String restoreScript() {
		return RESTORE_SCRIPT;
	}

	@Override
	public Map<String, Object> parameters() {
		return Map.of("keys", keys);
	}
}

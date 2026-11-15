package org.open4goods.api.services.aggregation.services.realtime.parser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Placeholders that datasources write instead of a value ("Donnée non spécifiée", "N/A", "NC"): the attribute is
 * absent, not invalid.
 *
 * <p>Entries are normalized: lower case, without accents, punctuation turned into spaces and spaces condensed
 * ("N/A" gives "n a"). Each user compares its own normalization of the raw value against them, and may add entries
 * of its own with {@link #with(String...)}.
 */
public final class EmptyValuePlaceholders {

	/** Placeholders shared by the realtime attribute aggregation and {@link WarrantyParser}. */
	public static final Set<String> COMMON = Set.of(
			"donnee non specifiee",
			"donnees non specifiees",
			"non specifie",
			"non specifiee",
			"n a",
			"na",
			"nc",
			"-");

	private EmptyValuePlaceholders() {
	}

	/**
	 * @param extra placeholders specific to the caller, already normalized
	 * @return an immutable set holding {@link #COMMON} and {@code extra}
	 */
	public static Set<String> with(String... extra) {
		Set<String> placeholders = new HashSet<>(COMMON);
		placeholders.addAll(List.of(extra));
		return Set.copyOf(placeholders);
	}
}

package org.open4goods.api.services.backfill;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Change of one product planned by a {@link BackfillOperation}: a painless update script that touches only the
 * fields of the operation, never a complete document (GOU-2266).
 *
 * @param script painless update script, run with {@code params}; it must set {@code ctx.op = 'noop'} when the
 *               product no longer needs the change
 * @param params script parameters
 * @param backup what the change removes or replaces, written to the JSONL backup before the update and given back
 *               as {@code params.data} to {@link BackfillOperation#restoreScript()}; null when nothing needs saving
 * @param labels what the change does, each one counted in the report (a removed key, a new value...)
 */
public record BackfillChange(String script, Map<String, Object> params, Map<String, Object> backup,
		List<String> labels) {

	public BackfillChange {
		Objects.requireNonNull(script, "script");
		params = params == null ? Map.of() : params;
		labels = labels == null ? List.of() : List.copyOf(labels);
	}
}

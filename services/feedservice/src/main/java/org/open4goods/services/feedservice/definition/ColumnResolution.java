package org.open4goods.services.feedservice.definition;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result of matching a feed's actual column headers against its {@link FeedDefinition}.
 *
 * @param matchedTargets header name to resolved {@link ColumnTarget}, for headers declared
 *        in {@link FeedDefinition#columnMappings()}
 * @param matchedUnitColumns header name to the quantity column it carries the unit for
 * @param missingUnitColumns quantity columns present whose configured unit column is absent
 *        from the actual headers
 * @param unknownColumns headers present in the source but declared nowhere in the feed
 *        definition; reported under {@link UnknownColumnPolicy#REPORT}, never guessed
 */
public record ColumnResolution(
        Map<String, ColumnTarget> matchedTargets,
        Map<String, String> matchedUnitColumns,
        Set<String> missingUnitColumns,
        Set<String> unknownColumns) {

    public ColumnResolution {
        matchedTargets = Map.copyOf(Objects.requireNonNull(matchedTargets, "matchedTargets must not be null"));
        matchedUnitColumns =
                Map.copyOf(Objects.requireNonNull(matchedUnitColumns, "matchedUnitColumns must not be null"));
        missingUnitColumns =
                Set.copyOf(Objects.requireNonNull(missingUnitColumns, "missingUnitColumns must not be null"));
        unknownColumns = Set.copyOf(Objects.requireNonNull(unknownColumns, "unknownColumns must not be null"));
    }

    /**
     * @return {@code true} when every header is accounted for and every configured unit
     *         column was actually present
     */
    public boolean isClean() {
        return unknownColumns.isEmpty() && missingUnitColumns.isEmpty();
    }
}

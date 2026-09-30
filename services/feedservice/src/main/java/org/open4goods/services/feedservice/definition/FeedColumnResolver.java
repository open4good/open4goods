package org.open4goods.services.feedservice.definition;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Matches a feed's actual CSV headers against its {@link FeedDefinition}.
 *
 * <p>This replaces label-guessing: a header either matches a column the feed
 * definition explicitly declares, or it is reported as unknown. There is no
 * fallback to a default-candidate list (AC2).
 */
public final class FeedColumnResolver {

    private FeedColumnResolver() {
    }

    /**
     * Resolves the actual headers of one feed read against its definition.
     *
     * @param definition feed definition to resolve against
     * @param headers actual column headers found in the source, as read
     * @return the resolution, including any unknown or missing-unit columns
     */
    public static ColumnResolution resolve(FeedDefinition definition, Set<String> headers) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(headers, "headers must not be null");

        Map<String, ColumnTarget> matchedTargets = new LinkedHashMap<>();
        Map<String, String> matchedUnitColumns = new LinkedHashMap<>();
        Set<String> unknownColumns = new LinkedHashSet<>();
        Set<String> missingUnitColumns = new LinkedHashSet<>();

        for (String header : headers) {
            ColumnTarget target = definition.columnMappings().get(header);
            if (target != null) {
                matchedTargets.put(header, target);
                continue;
            }
            if (definition.keyColumns().contains(header) || definition.unitColumns().containsValue(header)) {
                continue;
            }
            unknownColumns.add(header);
        }

        for (Map.Entry<String, String> unitColumn : definition.unitColumns().entrySet()) {
            String quantityColumn = unitColumn.getKey();
            String actualUnitColumn = unitColumn.getValue();
            if (headers.contains(actualUnitColumn)) {
                matchedUnitColumns.put(actualUnitColumn, quantityColumn);
            } else {
                missingUnitColumns.add(quantityColumn);
            }
        }

        return new ColumnResolution(matchedTargets, matchedUnitColumns, missingUnitColumns, unknownColumns);
    }
}

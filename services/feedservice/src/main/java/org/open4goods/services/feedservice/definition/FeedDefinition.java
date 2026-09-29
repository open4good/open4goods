package org.open4goods.services.feedservice.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.open4goods.datareference.model.SourceId;

/**
 * Stable, versioned coordinates and column contract for one merchant feed.
 *
 * <p>This is the resource AC1 requires: every field a feed version needs to be
 * read deterministically, with no display label standing in for an identity.
 * The feed's {@link SourceId} and {@link #providerSchemaVersion()} are the only
 * things a downstream store may key on; column names, on the other hand, are
 * provider vocabulary and may change under the same schema version.
 *
 * @param sourceId stable source identity; never a display label or merchant name
 * @param providerSchemaVersion provider schema version this definition was written against;
 *        a schema change creates a new version rather than mutating this one
 * @param keyColumns source-record key columns, in the source's own column names
 * @param language locale configured for this feed; never inferred from content
 * @param unitColumns explicit unit column per quantity column; the unit is kept as the
 *        provider wrote it, never normalized here
 * @param feedSemantics completion contract this feed version declares (AC4)
 * @param columnMappings explicit column name to {@link ColumnTarget} table (AC2)
 * @param unknownColumnPolicy policy for columns absent from {@link #columnMappings}; always
 *        {@link UnknownColumnPolicy#REPORT}
 */
public record FeedDefinition(
        SourceId sourceId,
        String providerSchemaVersion,
        List<String> keyColumns,
        Locale language,
        Map<String, String> unitColumns,
        FeedSemantics feedSemantics,
        Map<String, ColumnTarget> columnMappings,
        UnknownColumnPolicy unknownColumnPolicy) {

    /**
     * Validates identity, key and mapping shape.
     */
    public FeedDefinition {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        providerSchemaVersion = requireText(providerSchemaVersion, "providerSchemaVersion");
        keyColumns = List.copyOf(requireNonEmpty(keyColumns, "keyColumns"));
        Objects.requireNonNull(language, "language must not be null");
        unitColumns = Map.copyOf(Objects.requireNonNull(unitColumns, "unitColumns must not be null"));
        Objects.requireNonNull(feedSemantics, "feedSemantics must not be null");
        columnMappings = Map.copyOf(requireNonEmpty(columnMappings, "columnMappings"));
        Objects.requireNonNull(unknownColumnPolicy, "unknownColumnPolicy must not be null");

        for (String keyColumn : keyColumns) {
            requireText(keyColumn, "keyColumns entry");
        }
        for (Map.Entry<String, String> unit : unitColumns.entrySet()) {
            requireText(unit.getKey(), "unitColumns key");
            requireText(unit.getValue(), "unitColumns value");
        }
        for (Map.Entry<String, ColumnTarget> mapping : columnMappings.entrySet()) {
            requireText(mapping.getKey(), "columnMappings key");
            Objects.requireNonNull(mapping.getValue(), "columnMappings value must not be null");
        }
    }

    /**
     * Column names this definition declares meaning for: key columns, mapped columns and
     * their unit columns. Any other header encountered while reading a feed is unknown and
     * must be reported, never guessed (AC2).
     *
     * @return the full set of column names this definition recognizes
     */
    public Set<String> declaredColumns() {
        Set<String> declared = new LinkedHashSet<>(keyColumns);
        declared.addAll(columnMappings.keySet());
        declared.addAll(unitColumns.values());
        return Collections.unmodifiableSet(declared);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static <K, V> Map<K, V> requireNonEmpty(Map<K, V> map, String name) {
        Objects.requireNonNull(map, name + " must not be null");
        if (map.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return new LinkedHashMap<>(map);
    }

    private static List<String> requireNonEmpty(List<String> list, String name) {
        Objects.requireNonNull(list, name + " must not be null");
        if (list.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return list;
    }
}

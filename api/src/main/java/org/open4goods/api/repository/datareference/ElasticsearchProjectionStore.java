package org.open4goods.api.repository.datareference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;
import org.open4goods.datareference.port.ProjectionReadPort;
import org.open4goods.datareference.port.ProjectionWritePort;
import org.open4goods.datareference.port.ScanCursor;
import org.open4goods.datareference.port.ScanFailure;
import org.open4goods.datareference.port.ScanFailurePolicy;
import org.open4goods.datareference.port.ScanOrder;
import org.open4goods.datareference.port.ScanPage;
import org.open4goods.datareference.port.ScanRequest;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;

/**
 * Strict Elasticsearch storage for one complete, surface-separated GTIN projection.
 *
 * <p>The canonical envelope is persisted as non-indexed contract JSON. Only bounded,
 * consumer-facing fields are separately indexed, so neither raw assertions nor audit
 * candidate lists can create Elasticsearch mappings or accidentally power aggregation.
 */
@Repository
public class ElasticsearchProjectionStore implements ProjectionReadPort, ProjectionWritePort {

    /** Read alias for the GTIN projection index. */
    public static final String READ_ALIAS = "o4g-product-reference-projections-read";
    /** Write alias for the GTIN projection index. */
    public static final String WRITE_ALIAS = "o4g-product-reference-projections-write";

    private static final String VERSION = "v1";
    private static final String PIT_KEEP_ALIVE = "2m";

    private final ElasticsearchClient client;
    private volatile boolean indexReady;

    /**
     * Creates the projection persistence adapter.
     *
     * @param client configured Elasticsearch client
     */
    public ElasticsearchProjectionStore(ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    /** Writes the complete envelope atomically for one normalized GTIN. */
    @Override
    public void write(ProductReferenceProjectionEnvelope projection) {
        Objects.requireNonNull(projection, "projection must not be null");
        ensureIndex();
        try {
            client.index(index -> index.index(WRITE_ALIAS).id(projection.gtin().value()).requireAlias(true)
                    .refresh(Refresh.WaitFor).document(document(projection)));
        } catch (IOException exception) {
            throw storageFailure("write product-reference projection", exception);
        }
    }

    /** Writes every envelope independently, retaining safe identifiers for failures. */
    @Override
    public List<ScanFailure> writeAll(List<ProductReferenceProjectionEnvelope> projections) {
        Objects.requireNonNull(projections, "projections must not be null");
        List<ScanFailure> failures = new ArrayList<>();
        for (ProductReferenceProjectionEnvelope projection : projections) {
            if (projection == null) {
                failures.add(new ScanFailure("unknown", "projection must not be null"));
                continue;
            }
            try {
                write(projection);
            } catch (RuntimeException exception) {
                failures.add(new ScanFailure(projection.gtin().value(), sanitizedReason(exception)));
            }
        }
        return List.copyOf(failures);
    }

    /** Reads the complete three-surface envelope for a GTIN. */
    @Override
    public Optional<ProductReferenceProjectionEnvelope> find(Gtin gtin) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        ensureIndex();
        try {
            GetResponse<Map> response = client.get(get -> get.index(READ_ALIAS).id(gtin.value()), Map.class);
            return response.found() ? Optional.of(read(response.source())) : Optional.empty();
        } catch (IOException exception) {
            throw storageFailure("read product-reference projection", exception);
        }
    }

    /** Reads envelopes in the supplied coordinate order. */
    @Override
    public List<ProductReferenceProjectionEnvelope> findAll(List<Gtin> gtins) {
        Objects.requireNonNull(gtins, "gtins must not be null");
        return gtins.stream().map(gtin -> find(Objects.requireNonNull(gtin, "gtins must not contain null")))
                .flatMap(Optional::stream).toList();
    }

    /** Scans the strict index by GTIN using a point-in-time snapshot and search-after cursor. */
    @Override
    public ScanPage<ProductReferenceProjectionEnvelope> scan(ScanRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.order() != ScanOrder.RECORD_KEY) {
            throw new IllegalArgumentException("product-reference projections support only GTIN record-key order");
        }
        ensureIndex();
        CursorState cursor = request.cursor().isPresent() ? decodeCursor(request.cursor().orElseThrow()) : openCursor();
        try {
            var response = client.search(search -> {
                search.pit(pit -> pit.id(cursor.pitId()).keepAlive(keepAlive -> keepAlive.time(PIT_KEEP_ALIVE)))
                        .size(responseSize(request))
                        .sort(sort -> sort.field(field -> field.field("gtin").order(SortOrder.Asc)));
                if (!cursor.sortValues().isEmpty()) {
                    search.searchAfter(cursor.elasticSortValues());
                }
                return search;
            }, Map.class);
            List<Hit<Map>> hits = response.hits().hits();
            boolean hasMore = hits.size() > request.pageSize();
            List<Hit<Map>> pageHits = hasMore ? hits.subList(0, request.pageSize()) : hits;
            List<ProductReferenceProjectionEnvelope> elements = new ArrayList<>();
            List<ScanFailure> failures = new ArrayList<>();
            for (Hit<Map> hit : pageHits) {
                try {
                    elements.add(read(hit.source()));
                } catch (RuntimeException exception) {
                    if (request.failurePolicy() == ScanFailurePolicy.FAIL_FAST) {
                        closePit(cursor.pitId());
                        throw exception;
                    }
                    failures.add(new ScanFailure(hit.id(), sanitizedReason(exception)));
                }
            }
            if (!hasMore) {
                closePit(cursor.pitId());
                return new ScanPage<>(elements, Optional.empty(), failures);
            }
            List<CursorSort> sortValues = pageHits.getLast().sort().stream().map(CursorSort::from).toList();
            return new ScanPage<>(elements, Optional.of(encodeCursor(new CursorState(cursor.pitId(), sortValues))), failures);
        } catch (IOException exception) {
            closePit(cursor.pitId());
            throw storageFailure("scan product-reference projections", exception);
        }
    }

    private CursorState openCursor() {
        try {
            OpenPointInTimeResponse response = client.openPointInTime(open -> open.index(READ_ALIAS)
                    .keepAlive(keepAlive -> keepAlive.time(PIT_KEEP_ALIVE)));
            return new CursorState(response.id(), List.of());
        } catch (IOException exception) {
            throw storageFailure("open product-reference projection snapshot", exception);
        }
    }

    private int responseSize(ScanRequest request) {
        return request.pageSize() == ScanRequest.MAX_PAGE_SIZE ? request.pageSize() : request.pageSize() + 1;
    }

    private ProductReferenceProjectionEnvelope read(Map source) {
        try {
            return DataReferenceJson.mapper().readValue((String) source.get("projectionJson"),
                    ProductReferenceProjectionEnvelope.class);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("stored product-reference projection violates its serialization contract", exception);
        }
    }

    private Map<String, Object> document(ProductReferenceProjectionEnvelope projection) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("gtin", projection.gtin().value());
            document.put("projectionJson", DataReferenceJson.mapper().writeValueAsString(projection));
            for (ProjectionSurface surface : ProjectionSurface.values()) {
                ProductReferenceProjection component = projection.components().get(surface);
                String prefix = surfacePrefix(surface);
                document.put(prefix + "SearchTerms", component.search().lexicalTerms());
                document.put(prefix + "OfferCount", component.offers().activeOfferCount());
                document.put(prefix + "Available", component.offers().available());
                document.put(prefix + "EvaluationScoreCount", component.evaluation().scores().size());
            }
            return document;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("product-reference projection violates its serialization contract", exception);
        }
    }

    private synchronized void ensureIndex() {
        if (indexReady) {
            return;
        }
        try {
            String physical = physicalIndex(READ_ALIAS);
            if (physical == null) {
                client.indices().create(create -> create.index(versionedIndex()).mappings(mapping())
                        .aliases(READ_ALIAS, alias -> alias).aliases(WRITE_ALIAS, alias -> alias.isWriteIndex(true)));
            } else if (!physical.equals(versionedIndex())) {
                migrateAlias(physical);
            }
            indexReady = true;
        } catch (IOException exception) {
            throw storageFailure("create product-reference projection aliases", exception);
        }
    }

    private TypeMapping mapping() {
        TypeMapping.Builder builder = new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("gtin", property -> property.keyword(keyword -> keyword))
                .properties("projectionJson", property -> property.keyword(keyword -> keyword.index(false).docValues(false)));
        for (ProjectionSurface surface : ProjectionSurface.values()) {
            String prefix = surfacePrefix(surface);
            builder.properties(prefix + "SearchTerms", property -> property.text(text -> text));
            builder.properties(prefix + "OfferCount", property -> property.integer(number -> number));
            builder.properties(prefix + "Available", property -> property.boolean_(bool -> bool));
            builder.properties(prefix + "EvaluationScoreCount", property -> property.integer(number -> number));
        }
        return builder.build();
    }

    /**
     * Moves a versioned index without exposing a partially copied catalogue.
     *
     * <p>Writers switch first, then the old index is copied with create-only
     * semantics. A concurrent post-switch write therefore wins over its stale
     * source document before readers atomically move to the new index.
     */
    private void migrateAlias(String oldPhysical) {
        String target = versionedIndex();
        try {
            if (!client.indices().exists(exists -> exists.index(target)).value()) {
                client.indices().create(create -> create.index(target).mappings(mapping()));
            }
            String writePhysical = physicalIndex(WRITE_ALIAS);
            if (writePhysical == null || writePhysical.equals(oldPhysical)) {
                List<Action> actions = new ArrayList<>();
                if (writePhysical != null) {
                    actions.add(Action.of(action -> action.remove(remove -> remove.index(oldPhysical).alias(WRITE_ALIAS))));
                }
                actions.add(Action.of(action -> action.add(add -> add.index(target).alias(WRITE_ALIAS).isWriteIndex(true))));
                client.indices().updateAliases(update -> update.actions(actions));
            } else if (!writePhysical.equals(target)) {
                throw new IllegalStateException("product-reference projection write alias has an unexpected physical index");
            }
            // A legacy writer may have acknowledged a document before its next
            // scheduled refresh. Make that complete source view visible before
            // the one-time reindex so an alias migration cannot omit it.
            client.indices().refresh(refresh -> refresh.index(oldPhysical));
            client.reindex(reindex -> reindex.source(source -> source.index(oldPhysical))
                    .dest(destination -> destination.index(target).opType(co.elastic.clients.elasticsearch._types.OpType.Create))
                    .conflicts(Conflicts.Proceed).refresh(true));
            client.indices().updateAliases(update -> update
                    .actions(action -> action.remove(remove -> remove.index(oldPhysical).alias(READ_ALIAS)))
                    .actions(action -> action.add(add -> add.index(target).alias(READ_ALIAS))));
        } catch (IOException exception) {
            throw storageFailure("migrate product-reference projection aliases", exception);
        }
    }

    private String physicalIndex(String alias) {
        try {
            if (!client.indices().exists(exists -> exists.index(alias)).value()) {
                return null;
            }
            Set<String> indexes = client.indices().getAlias(get -> get.name(alias)).aliases().keySet();
            if (indexes.size() != 1) {
                throw new IllegalStateException("product-reference projection alias must resolve to exactly one physical index");
            }
            return indexes.iterator().next();
        } catch (IOException exception) {
            throw storageFailure("resolve product-reference projection alias", exception);
        }
    }

    private String surfacePrefix(ProjectionSurface surface) {
        return switch (surface) {
            case NUDGER_WEB -> "nudgerWeb";
            case B2B_API -> "b2bApi";
            case ODBL_EXPORT -> "odblExport";
        };
    }

    private String versionedIndex() {
        return READ_ALIAS + "-" + VERSION + "-000001";
    }

    private ScanCursor encodeCursor(CursorState cursor) {
        try {
            return new ScanCursor(Base64.getUrlEncoder().withoutPadding().encodeToString(
                    DataReferenceJson.mapper().writeValueAsBytes(cursor)));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("could not encode product-reference projection cursor", exception);
        }
    }

    private CursorState decodeCursor(ScanCursor cursor) {
        try {
            return DataReferenceJson.mapper().readValue(Base64.getUrlDecoder().decode(cursor.token()), CursorState.class);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("cursor is not a product-reference projection cursor", exception);
        }
    }

    private void closePit(String pitId) {
        try {
            ClosePointInTimeResponse response = client.closePointInTime(close -> close.id(pitId));
            if (!response.succeeded()) {
                throw new IllegalStateException("Elasticsearch did not acknowledge close of a projection snapshot");
            }
        } catch (IOException exception) {
            throw storageFailure("close product-reference projection snapshot", exception);
        }
    }

    private String sanitizedReason(RuntimeException exception) {
        return exception.getClass().getSimpleName();
    }

    private IllegalStateException storageFailure(String action, IOException exception) {
        return new IllegalStateException("could not " + action, exception);
    }

    /** Point-in-time state carried only by the opaque scan cursor. */
    private record CursorState(String pitId, List<CursorSort> sortValues) {
        private CursorState {
            Objects.requireNonNull(pitId, "pitId must not be null");
            sortValues = List.copyOf(Objects.requireNonNull(sortValues, "sortValues must not be null"));
        }

        List<FieldValue> elasticSortValues() {
            return sortValues.stream().map(CursorSort::toFieldValue).toList();
        }
    }

    /** Type-preserving JSON representation of one Elasticsearch search-after value. */
    private record CursorSort(String kind, String value) {
        private CursorSort {
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }

        static CursorSort from(FieldValue fieldValue) {
            return switch (fieldValue._kind()) {
                case String -> new CursorSort("string", fieldValue.stringValue());
                case Long -> new CursorSort("long", Long.toString(fieldValue.longValue()));
                case Double -> new CursorSort("double", Double.toString(fieldValue.doubleValue()));
                case Boolean -> new CursorSort("boolean", Boolean.toString(fieldValue.booleanValue()));
                default -> throw new IllegalStateException("unsupported product-reference cursor sort value kind: "
                        + fieldValue._kind());
            };
        }

        FieldValue toFieldValue() {
            return switch (kind) {
                case "string" -> FieldValue.of(value);
                case "long" -> FieldValue.of(Long.parseLong(value));
                case "double" -> FieldValue.of(Double.parseDouble(value));
                case "boolean" -> FieldValue.of(Boolean.parseBoolean(value));
                default -> throw new IllegalArgumentException("unsupported product-reference cursor sort kind: " + kind);
            };
        }
    }
}

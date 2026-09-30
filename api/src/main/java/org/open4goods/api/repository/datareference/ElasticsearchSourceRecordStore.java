package org.open4goods.api.repository.datareference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.port.ScanCursor;
import org.open4goods.datareference.port.ScanOrder;
import org.open4goods.datareference.port.ScanPage;
import org.open4goods.datareference.port.ScanRequest;
import org.open4goods.datareference.port.SourceRecordExpirySweeper;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.datareference.port.SourceRecordReplayScanner;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;

/**
 * Elasticsearch persistence adapter for source-record heads and their metadata-only transition
 * journal.
 *
 * <p>The head write is the transaction boundary Elasticsearch can actually provide. It embeds a
 * pending transition before the journal and recompute documents are created. Recovery writes both
 * deterministic side documents idempotently and clears the pending marker with compare-and-set;
 * a process loss at any point therefore cannot silently lose a projection invalidation.
 */
@Repository
public class ElasticsearchSourceRecordStore
        implements SourceRecordHeadStore, SourceRecordReplayScanner, SourceRecordExpirySweeper {

    /** Read alias for replaceable source-record heads. */
    public static final String HEAD_READ_ALIAS = "o4g-source-record-heads-read";
    /** Write alias for replaceable source-record heads. */
    public static final String HEAD_WRITE_ALIAS = "o4g-source-record-heads-write";
    /** Read alias for the immutable metadata journal. */
    public static final String JOURNAL_READ_ALIAS = "o4g-source-record-journal-read";
    /** Write alias for the immutable metadata journal. */
    public static final String JOURNAL_WRITE_ALIAS = "o4g-source-record-journal-write";
    /** Read alias for durable projection recompute work. */
    public static final String RECOMPUTE_READ_ALIAS = "o4g-source-recompute-read";
    /** Write alias for durable projection recompute work. */
    public static final String RECOMPUTE_WRITE_ALIAS = "o4g-source-recompute-write";

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSourceRecordStore.class);
    private static final String VERSION = "v2";
    private static final String PIT_KEEP_ALIVE = "2m";
    private static final int MAX_FIND_BY_GTIN = 10_000;

    private final ElasticsearchClient client;
    private final MeterRegistry meterRegistry;
    private volatile boolean indexesReady;

    /**
     * Creates the adapter.
     *
     * @param client configured Elasticsearch client
     */
    public ElasticsearchSourceRecordStore(ElasticsearchClient client) {
        this(client, Metrics.globalRegistry);
    }

    /**
     * Creates the Spring-managed adapter with the application metrics registry.
     *
     * @param client configured Elasticsearch client
     * @param meterRegistry application metrics registry
     */
    @Autowired
    public ElasticsearchSourceRecordStore(ElasticsearchClient client, MeterRegistry meterRegistry) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public SourceRecordTransition apply(SourceRecordMutation mutation) {
        Objects.requireNonNull(mutation, "mutation must not be null");
        ensureIndexes();
        SourceRecordHead candidate = mutation.candidate();
        String id = headId(candidate.key());

        Timer.Sample latency = Timer.start(meterRegistry);
        for (int attempt = 0; attempt < 16; attempt++) {
            StoredHead current = getStored(id);
            if (current != null && current.pending() != null) {
                deliverPending(current);
                continue;
            }

            Decision decision = decide(current, mutation);
            SourceRecordTransition transition = transitionFor(current, mutation, decision);
            if (decision == Decision.DUPLICATE) {
                increment("duplicate", candidate.key().sourceId());
                latency.stop(Timer.builder("o4g.source_record.write.latency")
                        .tag("source", candidate.key().sourceId().value()).register(meterRegistry));
                return transition;
            }
            SourceRecordHead storedHead = decision.replacesHead()
                    ? mergedHead(current == null ? null : current.head(), mutation)
                    : current == null ? candidate : current.head();
            if (!writePendingHead(id, current, storedHead, transition)) {
                increment("optimistic_conflict", candidate.key().sourceId());
                continue;
            }

            StoredHead pending = getStored(id);
            if (pending != null && pending.pending() != null && pending.pending().id().equals(transition.id())) {
                deliverPending(pending);
            }
            // A concurrent writer may have completed delivery and cleared this
            // pending marker before this writer rereads it. It cannot admit a
            // following transition until the deterministic side documents are
            // durable, so a missing or newer marker means this transition was
            // already delivered successfully.
            increment(transition.outcome().name().toLowerCase(java.util.Locale.ROOT), candidate.key().sourceId());
            latency.stop(Timer.builder("o4g.source_record.write.latency").tag("source", candidate.key().sourceId().value())
                    .register(meterRegistry));
            return transition;
        }
        throw new IllegalStateException("source-record head remained concurrently modified after 16 retries");
    }

    @Override
    public Optional<SourceRecordHead> find(SourceRecordKey key) {
        Objects.requireNonNull(key, "key must not be null");
        ensureIndexes();
        StoredHead stored = getStored(headId(key));
        if (stored != null && stored.pending() != null) {
            deliverPending(stored);
            stored = getStored(headId(key));
        }
        return Optional.ofNullable(stored).map(StoredHead::head);
    }

    @Override
    public List<SourceRecordHead> findByGtin(Gtin gtin) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        ensureIndexes();
        ScanRequest request = ScanRequest.first(Math.min(1_000, MAX_FIND_BY_GTIN));
        List<SourceRecordHead> result = new ArrayList<>();
        do {
            ScanPage<SourceRecordHead> page = scanByGtin(gtin, request);
            result.addAll(page.elements());
            if (result.size() > MAX_FIND_BY_GTIN) {
                cancel(page.nextCursor().orElseThrow());
                throw new IllegalStateException("GTIN has more than " + MAX_FIND_BY_GTIN + " source heads; use replay scan");
            }
            if (page.nextCursor().isEmpty()) {
                return List.copyOf(result);
            }
            request = request.resumeAt(page.nextCursor().orElseThrow());
        } while (true);
    }

    @Override
    public boolean storeIfNewer(SourceRecordHead head) {
        SourceRecordTransitionOutcome outcome = apply(SourceRecordMutation.full(head)).outcome();
        return outcome == SourceRecordTransitionOutcome.ACCEPTED
                || outcome == SourceRecordTransitionOutcome.TOMBSTONED;
    }

    @Override
    public boolean delete(SourceRecordKey key) {
        Objects.requireNonNull(key, "key must not be null");
        ensureIndexes();
        try {
            return client.delete(delete -> delete.index(HEAD_WRITE_ALIAS).id(headId(key))).result()
                    .jsonValue().equals("deleted");
        } catch (ElasticsearchException exception) {
            if (exception.status() == 404) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("delete source-record head", exception);
        }
    }

    @Override
    public ScanPage<SourceRecordHead> scanAll(ScanRequest request) {
        return scan(null, null, request);
    }

    @Override
    public ScanPage<SourceRecordHead> scanBySource(SourceId sourceId, ScanRequest request) {
        return scan(Objects.requireNonNull(sourceId, "sourceId must not be null"), null, request);
    }

    @Override
    public ScanPage<SourceRecordHead> scanByGtin(Gtin gtin, ScanRequest request) {
        return scan(null, Objects.requireNonNull(gtin, "gtin must not be null"), request);
    }

    @Override
    public void cancel(ScanCursor cursor) {
        Objects.requireNonNull(cursor, "cursor must not be null");
        CursorState state = decodeCursor(cursor);
        try {
            ClosePointInTimeResponse response = client.closePointInTime(close -> close.id(state.pitId()));
            if (!response.succeeded()) {
                LOGGER.warn("Elasticsearch did not acknowledge close of a source replay snapshot");
            }
        } catch (ElasticsearchException exception) {
            if (exception.status() != 404) {
                throw exception;
            }
        } catch (IOException exception) {
            throw storageFailure("cancel source-record replay", exception);
        }
    }

    /**
     * Delivers every pending transition left by an interrupted process.
     *
     * <p>This method uses bounded {@code search_after} pages and is intentionally callable by a
     * startup runner; it never opens a scroll or materializes the catalogue.
     */
    public void recoverPendingTransitions() {
        ensureIndexes();
        String after = null;
        do {
            try {
                String searchAfter = after;
                var response = client.search(search -> {
                    search.index(HEAD_READ_ALIAS).size(500).seqNoPrimaryTerm(true)
                            .query(query -> query.term(term -> term.field("hasPending").value(true)))
                            .sort(sort -> sort.field(field -> field.field("recordKey").order(SortOrder.Asc)));
                    if (searchAfter != null) {
                        search.searchAfter(searchAfter);
                    }
                    return search;
                }, Map.class);
                List<Hit<Map>> hits = response.hits().hits();
                if (hits.isEmpty()) {
                    return;
                }
                for (Hit<Map> hit : hits) {
                    deliverPending(stored(hit));
                }
                after = (String) hits.getLast().source().get("recordKey");
            } catch (IOException exception) {
                throw storageFailure("recover pending source-record transitions", exception);
            }
        } while (true);
    }

    @Override
    public int enqueueExpired(Instant asOf) {
        Objects.requireNonNull(asOf, "asOf must not be null");
        ensureIndexes();
        String after = null;
        int enqueuedHeads = 0;
        do {
            try {
                String searchAfter = after;
                var response = client.search(search -> {
                    search.index(HEAD_READ_ALIAS).size(500).seqNoPrimaryTerm(true)
                            .query(query -> query.bool(bool -> bool
                                    .filter(filter -> filter.term(term -> term.field("state").value("ACTIVE")))
                                    .filter(filter -> filter.range(range -> range.date(date -> date
                                            .field("expiresAt").lt(asOf.toString()))))))
                            .sort(sort -> sort.field(field -> field.field("recordKey").order(SortOrder.Asc)));
                    if (searchAfter != null) {
                        search.searchAfter(searchAfter);
                    }
                    return search;
                }, Map.class);
                List<Hit<Map>> hits = response.hits().hits();
                if (hits.isEmpty()) {
                    return enqueuedHeads;
                }
                for (Hit<Map> hit : hits) {
                    StoredHead stored = stored(hit);
                    if (stored.pending() != null) {
                        deliverPending(stored);
                    }
                    if (enqueueExpiryRemoval(stored)) {
                        increment("expired", stored.head().key().sourceId());
                        enqueuedHeads++;
                    }
                }
                after = (String) hits.getLast().source().get("recordKey");
            } catch (IOException exception) {
                throw storageFailure("enqueue expired source-record heads", exception);
            }
        } while (true);
    }

    /**
     * Finishes any accepted transition left between the head CAS and side-document delivery.
     *
     * <p>The listener runs after Elasticsearch client configuration has completed, rather than in
     * the repository constructor where a transient startup ordering failure could prevent the
     * application context from being created.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingTransitionsAfterStartup() {
        try {
            recoverPendingTransitions();
        } catch (IllegalStateException exception) {
            // Local tooling and API-only test contexts may deliberately start
            // without Elasticsearch. Normal writes still recover pending work
            // before admitting a transition once the store becomes available.
            LOGGER.warn("Source-record pending transition recovery is deferred: {}", exception.getMessage());
        }
    }

    private ScanPage<SourceRecordHead> scan(SourceId sourceId, Gtin gtin, ScanRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ensureIndexes();
        CursorState state = request.cursor().map(this::decodeCursor).orElseGet(() -> openCursor(request.order()));
        try {
            var response = client.search(search -> {
                search.pit(pit -> pit.id(state.pitId()).keepAlive(keepAlive -> keepAlive.time(PIT_KEEP_ALIVE)))
                        .size(request.pageSize()).seqNoPrimaryTerm(true)
                        .query(query -> sourceId != null
                                ? query.term(term -> term.field("sourceId").value(sourceId.value()))
                                : gtin != null
                                        ? query.term(term -> term.field("gtins").value(gtin.value()))
                                        : query.matchAll(match -> match))
                        .sort(sortOptions(request.order()));
                if (!state.sortValues().isEmpty()) {
                    search.searchAfter(state.sortValues().stream().map(CursorSort::toFieldValue).toList());
                }
                return search;
            }, Map.class);
            List<Hit<Map>> hits = response.hits().hits();
            List<SourceRecordHead> heads = new ArrayList<>(hits.size());
            for (Hit<Map> hit : hits) {
                StoredHead stored = stored(hit);
                if (stored.pending() != null) {
                    deliverPending(stored);
                }
                heads.add(stored.head());
                recordReplayMetrics(stored.head());
            }
            if (hits.size() < request.pageSize()) {
                closePit(state.pitId());
                return ScanPage.last(heads);
            }
            List<CursorSort> sorts = hits.getLast().sort().stream().map(CursorSort::from).toList();
            return new ScanPage<>(heads, Optional.of(encodeCursor(new CursorState(state.pitId(), sorts))), List.of());
        } catch (IOException exception) {
            throw storageFailure("scan source-record heads", exception);
        }
    }

    private List<SortOptions> sortOptions(ScanOrder order) {
        return switch (order) {
            case RECORD_KEY -> List.of(
                    SortOptions.of(sort -> sort.field(field -> field.field("recordKey").order(SortOrder.Asc))));
            case OBSERVED_AT -> List.of(
                    SortOptions.of(sort -> sort.field(field -> field.field("observedAt").order(SortOrder.Asc))),
                    SortOptions.of(sort -> sort.field(field -> field.field("recordKey").order(SortOrder.Asc))));
        };
    }

    private CursorState openCursor(ScanOrder order) {
        try {
            OpenPointInTimeResponse response = client.openPointInTime(open -> open.index(HEAD_READ_ALIAS)
                    .keepAlive(keepAlive -> keepAlive.time(PIT_KEEP_ALIVE)));
            return new CursorState(response.id(), List.of());
        } catch (IOException exception) {
            throw storageFailure("open source-record replay snapshot", exception);
        }
    }

    private Decision decide(StoredHead current, SourceRecordMutation mutation) {
        SourceRecordHead candidate = mutation.candidate();
        if (current == null) {
            if (candidate.state() == SourceRecordState.DELETED) {
                return Decision.TOMBSTONED;
            }
            if (candidate.state() == SourceRecordState.UNAVAILABLE || candidate.state() == SourceRecordState.REJECTED) {
                return Decision.INITIAL_REJECTED;
            }
            return Decision.ACCEPTED;
        }
        SourceRecordHead stored = current.head();
        if (stored.payloadHash().equals(candidate.payloadHash())) {
            return Decision.DUPLICATE;
        }
        int observation = candidate.observedAt().compareTo(stored.observedAt());
        if (observation < 0) {
            return Decision.OUT_OF_ORDER;
        }
        int retrieval = candidate.retrievedAt().compareTo(stored.retrievedAt());
        if (observation == 0 && retrieval < 0) {
            return Decision.OUT_OF_ORDER;
        }
        if (observation == 0 && retrieval == 0) {
            return Decision.COLLISION;
        }
        if ((candidate.state() == SourceRecordState.UNAVAILABLE || candidate.state() == SourceRecordState.REJECTED)
                && stored.isUsableAt(candidate.retrievedAt())) {
            return Decision.REJECTED;
        }
        return candidate.state() == SourceRecordState.DELETED ? Decision.TOMBSTONED : Decision.ACCEPTED;
    }

    private SourceRecordTransition transitionFor(StoredHead current, SourceRecordMutation mutation, Decision decision) {
        long revision = decision == Decision.DUPLICATE && current != null
                ? current.revision()
                : current == null ? 1 : current.revision() + 1;
        SourceRecordHead candidate = mutation.candidate();
        List<Gtin> affected = affectedGtins(current == null ? List.of() : current.head().gtinLinks(), candidate.gtinLinks());
        return new SourceRecordTransition(SourceRecordTransition.idFor(candidate.key(), revision), candidate.key(), revision,
                candidate.schemaVersion(), candidate.providerVersion(), current == null ? null : current.head().payloadHash(),
                candidate.payloadHash(), candidate.observedAt(), candidate.retrievedAt(), candidate.state(), decision.outcome(),
                mutation.sanitizedErrorCode(), affected);
    }

    private SourceRecordHead mergedHead(SourceRecordHead current, SourceRecordMutation mutation) {
        SourceRecordHead candidate = mutation.candidate();
        if (current == null || candidate.completeness() == SourceRecordCompleteness.FULL
                || candidate.state() == SourceRecordState.DELETED) {
            return candidate;
        }
        LinkedHashMap<SourceAssertion.Coordinate, SourceAssertion> assertions = new LinkedHashMap<>();
        current.assertions().forEach(assertion -> assertions.put(assertion.coordinate(), assertion));
        mutation.tombstones().forEach(assertions::remove);
        candidate.assertions().forEach(assertion -> assertions.put(assertion.coordinate(), assertion));
        LinkedHashMap<Gtin, GtinLink> links = new LinkedHashMap<>();
        current.gtinLinks().forEach(link -> links.put(link.gtin(), link));
        mutation.withdrawnGtins().forEach(links::remove);
        candidate.gtinLinks().forEach(link -> links.put(link.gtin(), link));
        return new SourceRecordHead(candidate.key(), candidate.schemaVersion(), candidate.providerVersion(), candidate.observedAt(),
                candidate.retrievedAt(), candidate.expiresAt(), candidate.completeness(), candidate.state(), candidate.payloadHash(),
                candidate.evidenceReference(), candidate.usagePolicyRef(), List.copyOf(links.values()),
                List.copyOf(assertions.values()));
    }

    private boolean writePendingHead(String id, StoredHead current, SourceRecordHead head, SourceRecordTransition transition) {
        try {
            Map<String, Object> document = headDocument(head, transition.revision(), transition);
            if (current == null) {
                client.index(index -> index.index(HEAD_WRITE_ALIAS).id(id).opType(OpType.Create).requireAlias(true)
                        .refresh(Refresh.WaitFor)
                        .document(document));
            } else {
                client.index(index -> index.index(HEAD_WRITE_ALIAS).id(id).requireAlias(true).ifSeqNo(current.seqNo())
                        .ifPrimaryTerm(current.primaryTerm()).refresh(Refresh.WaitFor).document(document));
            }
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("compare-and-set source-record head", exception);
        }
    }

    private void deliverPending(StoredHead stored) {
        SourceRecordTransition transition = stored.pending();
        writeImmutable(JOURNAL_WRITE_ALIAS, transition.id(), journalDocument(transition));
        for (Gtin gtin : transition.affectedGtins()) {
            writeImmutable(RECOMPUTE_WRITE_ALIAS, transition.id() + ":" + gtin.value(),
                    Map.of("transitionId", transition.id(), "gtin", gtin.value(), "sourceId", transition.key().sourceId().value(),
                            "revision", transition.revision(), "kind", "TRANSITION"));
        }
        clearPending(stored, transition.id());
    }

    private boolean enqueueExpiryRemoval(StoredHead stored) {
        SourceRecordHead head = stored.head();
        if (head.expiresAt() == null || head.gtinLinks().isEmpty()) {
            return false;
        }
        String expiryId = expiryId(head);
        boolean enqueued = false;
        for (GtinLink link : head.gtinLinks()) {
            enqueued |= writeImmutable(RECOMPUTE_WRITE_ALIAS, expiryId + ":" + link.gtin().value(),
                    Map.of("transitionId", expiryId, "gtin", link.gtin().value(), "sourceId", head.key().sourceId().value(),
                            "revision", stored.revision(), "kind", "EXPIRY"));
        }
        return enqueued;
    }

    private boolean writeImmutable(String alias, String id, Map<String, Object> document) {
        try {
            client.index(index -> index.index(alias).id(id).opType(OpType.Create).requireAlias(true).refresh(Refresh.WaitFor)
                    .document(document));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() != 409) {
                throw exception;
            }
            return false;
        } catch (IOException exception) {
            throw storageFailure("write immutable source-record transition", exception);
        }
    }

    private void clearPending(StoredHead stored, String transitionId) {
        try {
            StoredHead current = getStored(stored.id());
            if (current == null || current.pending() == null || !current.pending().id().equals(transitionId)) {
                return;
            }
            client.index(index -> index.index(HEAD_WRITE_ALIAS).id(current.id()).requireAlias(true).ifSeqNo(current.seqNo())
                    .ifPrimaryTerm(current.primaryTerm()).refresh(Refresh.WaitFor)
                    .document(headDocument(current.head(), current.revision(), null)));
        } catch (ElasticsearchException exception) {
            if (exception.status() != 409) {
                throw exception;
            }
        } catch (IOException exception) {
            throw storageFailure("clear pending source-record transition", exception);
        }
    }

    private StoredHead getStored(String id) {
        try {
            GetResponse<Map> response = client.get(get -> get.index(HEAD_READ_ALIAS).id(id), Map.class);
            return response.found() ? stored(id, response.source(), response.seqNo(), response.primaryTerm()) : null;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 404) {
                return null;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("read source-record head", exception);
        }
    }

    private StoredHead stored(Hit<Map> hit) {
        return stored(hit.id(), hit.source(), hit.seqNo(), hit.primaryTerm());
    }

    private StoredHead stored(String id, Map source, long seqNo, long primaryTerm) {
        try {
            SourceRecordHead head = DataReferenceJson.mapper().readValue((String) source.get("headJson"), SourceRecordHead.class);
            Object pendingJson = source.get("pendingJson");
            SourceRecordTransition pending = pendingJson == null ? null
                    : DataReferenceJson.mapper().readValue((String) pendingJson, SourceRecordTransition.class);
            return new StoredHead(id, head, ((Number) source.get("revision")).longValue(), pending, seqNo, primaryTerm);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("stored source-record document violates its serialization contract", exception);
        }
    }

    private Map<String, Object> headDocument(SourceRecordHead head, long revision, SourceRecordTransition pending) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("sourceId", head.key().sourceId().value());
            document.put("recordId", head.key().sourceRecordId().value());
            document.put("recordKey", head.key().externalForm());
            document.put("gtins", head.gtinLinks().stream().map(link -> link.gtin().value()).toList());
            document.put("observedAt", head.observedAt().toString());
            document.put("retrievedAt", head.retrievedAt().toString());
            if (head.expiresAt() != null) {
                document.put("expiresAt", head.expiresAt().toString());
            }
            document.put("state", head.state().name());
            document.put("payloadHash", head.payloadHash().algorithm() + ":" + head.payloadHash().hexadecimalValue());
            document.put("revision", revision);
            document.put("headJson", DataReferenceJson.mapper().writeValueAsString(head));
            document.put("hasPending", pending != null);
            if (pending != null) {
                document.put("pendingJson", DataReferenceJson.mapper().writeValueAsString(pending));
            }
            return document;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("source-record head violates its serialization contract", exception);
        }
    }

    private Map<String, Object> journalDocument(SourceRecordTransition transition) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("transitionId", transition.id());
        document.put("sourceId", transition.key().sourceId().value());
        document.put("recordKey", transition.key().externalForm());
        document.put("revision", transition.revision());
        document.put("schemaVersion", transition.schemaVersion());
        if (transition.providerVersion() != null) {
            document.put("providerVersion", transition.providerVersion());
        }
        if (transition.oldHash() != null) {
            document.put("oldHash", transition.oldHash().algorithm() + ":" + transition.oldHash().hexadecimalValue());
        }
        document.put("newHash", transition.newHash().algorithm() + ":" + transition.newHash().hexadecimalValue());
        document.put("observedAt", transition.observedAt().toString());
        document.put("retrievedAt", transition.retrievedAt().toString());
        document.put("state", transition.state().name());
        document.put("outcome", transition.outcome().name());
        if (transition.sanitizedErrorCode() != null) {
            document.put("sanitizedErrorCode", transition.sanitizedErrorCode());
        }
        return document;
    }

    private List<Gtin> affectedGtins(List<GtinLink> before, List<GtinLink> after) {
        Set<Gtin> affected = new LinkedHashSet<>();
        before.forEach(link -> affected.add(link.gtin()));
        after.forEach(link -> affected.add(link.gtin()));
        return List.copyOf(affected);
    }

    private synchronized void ensureIndexes() {
        if (indexesReady) {
            return;
        }
        ensureIndex(HEAD_READ_ALIAS, HEAD_WRITE_ALIAS, headMapping());
        ensureIndex(JOURNAL_READ_ALIAS, JOURNAL_WRITE_ALIAS, journalMapping());
        ensureIndex(RECOMPUTE_READ_ALIAS, RECOMPUTE_WRITE_ALIAS, recomputeMapping());
        indexesReady = true;
    }

    private void ensureIndex(String readAlias, String writeAlias, TypeMapping mapping) {
        try {
            String physical = physicalIndex(readAlias);
            if (physical == null) {
                client.indices().create(create -> create.index(versionedIndex(readAlias)).mappings(mapping)
                        .aliases(readAlias, alias -> alias)
                        .aliases(writeAlias, alias -> alias.isWriteIndex(true)));
                return;
            }
            if (!physical.equals(versionedIndex(readAlias))) {
                migrateAlias(readAlias, writeAlias, physical, mapping);
            }
        } catch (ElasticsearchException exception) {
            if (exception.status() != 400 && exception.status() != 409) {
                throw exception;
            }
        } catch (IOException exception) {
            throw storageFailure("create source-record aliases", exception);
        }
    }

    /**
     * Moves one versioned alias without losing concurrent writes.
     *
     * <p>Writers move to the fresh physical index first. The old index is then copied with
     * create-only semantics, so a write accepted after that cutover wins over its stale old
     * version. Readers remain on the old index during the copy and switch atomically only when
     * it has caught up. They can be briefly stale but never observe a partial catalogue.
     */
    private void migrateAlias(String readAlias, String writeAlias, String oldPhysical, TypeMapping mapping) {
        String target = versionedIndex(readAlias);
        try {
            if (!client.indices().exists(exists -> exists.index(target)).value()) {
                client.indices().create(create -> create.index(target).mappings(mapping));
            }
            String writePhysical = physicalIndex(writeAlias);
            if (writePhysical == null || writePhysical.equals(oldPhysical)) {
                List<Action> actions = new ArrayList<>();
                if (writePhysical != null) {
                    actions.add(Action.of(action -> action.remove(remove -> remove.index(oldPhysical).alias(writeAlias))));
                }
                actions.add(Action.of(action -> action.add(add -> add.index(target).alias(writeAlias).isWriteIndex(true))));
                client.indices().updateAliases(update -> update.actions(actions));
            } else if (!writePhysical.equals(target)) {
                throw new IllegalStateException("source-record write alias has an unexpected physical index");
            }
            client.reindex(reindex -> reindex.source(source -> source.index(oldPhysical))
                    .dest(destination -> destination.index(target).opType(OpType.Create)).conflicts(Conflicts.Proceed)
                    .refresh(true));
            client.indices().updateAliases(update -> update
                    .actions(action -> action.remove(remove -> remove.index(oldPhysical).alias(readAlias)))
                    .actions(action -> action.add(add -> add.index(target).alias(readAlias))));
        } catch (IOException exception) {
            throw storageFailure("migrate source-record alias", exception);
        }
    }

    private String physicalIndex(String alias) {
        try {
            if (!client.indices().exists(exists -> exists.index(alias)).value()) {
                return null;
            }
            Set<String> indexes = client.indices().getAlias(get -> get.name(alias)).aliases().keySet();
            if (indexes.size() != 1) {
                throw new IllegalStateException("source-record alias must resolve to exactly one physical index");
            }
            return indexes.iterator().next();
        } catch (IOException exception) {
            throw storageFailure("resolve source-record alias", exception);
        }
    }

    private String versionedIndex(String readAlias) {
        return readAlias + "-" + VERSION + "-000001";
    }

    private TypeMapping headMapping() {
        return new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("sourceId", property -> property.keyword(keyword -> keyword))
                .properties("recordId", property -> property.keyword(keyword -> keyword))
                .properties("recordKey", property -> property.keyword(keyword -> keyword))
                .properties("gtins", property -> property.keyword(keyword -> keyword))
                .properties("observedAt", property -> property.dateNanos(date -> date))
                .properties("retrievedAt", property -> property.dateNanos(date -> date))
                .properties("expiresAt", property -> property.dateNanos(date -> date))
                .properties("state", property -> property.keyword(keyword -> keyword))
                .properties("payloadHash", property -> property.keyword(keyword -> keyword))
                .properties("revision", property -> property.long_(number -> number))
                .properties("headJson", property -> property.keyword(keyword -> keyword.index(false).docValues(false)))
                .properties("hasPending", property -> property.boolean_(bool -> bool))
                .properties("pendingJson", property -> property.keyword(keyword -> keyword.index(false).docValues(false)))
                .build();
    }

    private TypeMapping journalMapping() {
        return new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("transitionId", property -> property.keyword(keyword -> keyword))
                .properties("sourceId", property -> property.keyword(keyword -> keyword))
                .properties("recordKey", property -> property.keyword(keyword -> keyword))
                .properties("revision", property -> property.long_(number -> number))
                .properties("schemaVersion", property -> property.keyword(keyword -> keyword))
                .properties("providerVersion", property -> property.keyword(keyword -> keyword))
                .properties("oldHash", property -> property.keyword(keyword -> keyword))
                .properties("newHash", property -> property.keyword(keyword -> keyword))
                .properties("observedAt", property -> property.dateNanos(date -> date))
                .properties("retrievedAt", property -> property.dateNanos(date -> date))
                .properties("state", property -> property.keyword(keyword -> keyword))
                .properties("outcome", property -> property.keyword(keyword -> keyword))
                .properties("sanitizedErrorCode", property -> property.keyword(keyword -> keyword))
                .build();
    }

    private TypeMapping recomputeMapping() {
        return new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("transitionId", property -> property.keyword(keyword -> keyword))
                .properties("gtin", property -> property.keyword(keyword -> keyword))
                .properties("sourceId", property -> property.keyword(keyword -> keyword))
                .properties("revision", property -> property.long_(number -> number))
                .properties("kind", property -> property.keyword(keyword -> keyword))
                .build();
    }

    private String headId(SourceRecordKey key) {
        try {
            return "source-head:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.externalForm().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private String expiryId(SourceRecordHead head) {
        try {
            String value = head.key().externalForm() + "\u0000" + head.payloadHash().algorithm() + ":"
                    + head.payloadHash().hexadecimalValue() + "\u0000" + head.expiresAt();
            return "source-expiry:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private ScanCursor encodeCursor(CursorState state) {
        try {
            return new ScanCursor(Base64.getUrlEncoder().withoutPadding().encodeToString(
                    DataReferenceJson.mapper().writeValueAsBytes(state)));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("could not encode source-record scan cursor", exception);
        }
    }

    private CursorState decodeCursor(ScanCursor cursor) {
        try {
            return DataReferenceJson.mapper().readValue(Base64.getUrlDecoder().decode(cursor.token()), CursorState.class);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("cursor is not a source-record replay cursor", exception);
        }
    }

    private void closePit(String pitId) {
        try {
            client.closePointInTime(close -> close.id(pitId));
        } catch (IOException exception) {
            throw storageFailure("close source-record replay snapshot", exception);
        }
    }

    private IllegalStateException storageFailure(String action, IOException exception) {
        return new IllegalStateException("could not " + action, exception);
    }

    private void increment(String outcome, SourceId sourceId) {
        Counter.builder("o4g.source_record.transitions").tag("source", sourceId.value()).tag("outcome", outcome)
                .register(meterRegistry).increment();
    }

    private void recordReplayMetrics(SourceRecordHead head) {
        Instant now = Instant.now();
        Timer.builder("o4g.source_record.replay.lag").tag("source", head.key().sourceId().value()).register(meterRegistry)
                .record(java.time.Duration.between(head.observedAt(), now).isNegative()
                        ? java.time.Duration.ZERO : java.time.Duration.between(head.observedAt(), now));
        if (head.state() == SourceRecordState.ACTIVE && head.expiresAt() != null && now.isAfter(head.expiresAt())) {
            increment("expired", head.key().sourceId());
        }
    }

    private enum Decision {
        ACCEPTED(SourceRecordTransitionOutcome.ACCEPTED, true),
        INITIAL_REJECTED(SourceRecordTransitionOutcome.REJECTED, true),
        DUPLICATE(SourceRecordTransitionOutcome.DUPLICATE, false),
        OUT_OF_ORDER(SourceRecordTransitionOutcome.OUT_OF_ORDER, false),
        COLLISION(SourceRecordTransitionOutcome.COLLISION, false),
        TOMBSTONED(SourceRecordTransitionOutcome.TOMBSTONED, true),
        REJECTED(SourceRecordTransitionOutcome.REJECTED, false);

        private final SourceRecordTransitionOutcome outcome;
        private final boolean replacesHead;

        Decision(SourceRecordTransitionOutcome outcome, boolean replacesHead) {
            this.outcome = outcome;
            this.replacesHead = replacesHead;
        }

        SourceRecordTransitionOutcome outcome() {
            return outcome;
        }

        boolean replacesHead() {
            return replacesHead;
        }
    }

    private record StoredHead(String id, SourceRecordHead head, long revision, SourceRecordTransition pending,
            long seqNo, long primaryTerm) {
    }

    private record CursorState(String pitId, List<CursorSort> sortValues) {
        private CursorState {
            Objects.requireNonNull(pitId, "pitId must not be null");
            sortValues = List.copyOf(Objects.requireNonNull(sortValues, "sortValues must not be null"));
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
                default -> throw new IllegalStateException("unsupported source-record sort value kind: " + fieldValue._kind());
            };
        }

        FieldValue toFieldValue() {
            return switch (kind) {
                case "string" -> FieldValue.of(value);
                case "long" -> FieldValue.of(Long.parseLong(value));
                case "double" -> FieldValue.of(Double.parseDouble(value));
                case "boolean" -> FieldValue.of(Boolean.parseBoolean(value));
                default -> throw new IllegalArgumentException("unsupported source-record cursor sort kind: " + kind);
            };
        }
    }
}

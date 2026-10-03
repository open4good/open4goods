package org.open4goods.api.repository.pricehistory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.pricehistory.model.OfferHead;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.port.OfferHeadStore;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

/**
 * Elasticsearch persistence adapter for the versioned {@code OfferHead} index described by
 * ADR-0011: the replaceable latest state of one provider offer, keyed by
 * {@code (gtin, providerId, providerOfferId)}.
 *
 * <p>{@link OfferHead} already embeds its own revision and any pending price-change event
 * (unlike the source-record head/transition split), so one document fully represents the store's
 * compare-and-set state; no separate pending-document recovery is needed here. Concurrency follows
 * the same alias/seqNo/primaryTerm convention as {@code ElasticsearchIngestionCheckpointStore}.
 */
@Repository
public class ElasticsearchOfferHeadStore implements OfferHeadStore {

    /** Read alias for current provider offer heads. */
    public static final String READ_ALIAS = "o4g-offer-heads-read";
    /** Write alias for current provider offer heads. */
    public static final String WRITE_ALIAS = "o4g-offer-heads-write";

    private static final String VERSION = "v1";
    private static final String PIT_KEEP_ALIVE = "2m";
    private static final int PROVIDER_SCAN_PAGE_SIZE = 500;

    private final ElasticsearchClient client;
    private volatile boolean indexReady;

    /**
     * Creates the offer-head persistence adapter.
     *
     * @param client configured Elasticsearch client
     */
    public ElasticsearchOfferHeadStore(ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Optional<OfferHead> find(OfferKey key) {
        Objects.requireNonNull(key, "key must not be null");
        ensureIndex();
        return Optional.ofNullable(stored(idFor(key))).map(StoredHead::head);
    }

    @Override
    public boolean compareAndSet(OfferHead head, long expectedRevision) {
        Objects.requireNonNull(head, "head must not be null");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must not be negative");
        }
        ensureIndex();
        String id = idFor(head.observation().key());
        StoredHead current = stored(id);
        if (current == null) {
            if (expectedRevision != 0) {
                return false;
            }
            return create(id, head);
        }
        if (current.revision() != expectedRevision) {
            return false;
        }
        return replace(current, head);
    }

    @Override
    public Stream<OfferHead> findByProvider(SourceId providerId) {
        Objects.requireNonNull(providerId, "providerId must not be null");
        ensureIndex();
        OpenPointInTimeResponse opened;
        try {
            opened = client.openPointInTime(open -> open.index(READ_ALIAS).keepAlive(keepAlive -> keepAlive.time(PIT_KEEP_ALIVE)));
        } catch (IOException exception) {
            throw storageFailure("open offer-head provider scan", exception);
        }
        ProviderHeadIterator iterator = new ProviderHeadIterator(opened.id(), providerId);
        Stream<OfferHead> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
        return stream.onClose(() -> closePit(iterator.pitId()));
    }

    private boolean create(String id, OfferHead head) {
        try {
            client.index(index -> index.index(WRITE_ALIAS).id(id).opType(OpType.Create).requireAlias(true)
                    .refresh(Refresh.WaitFor).document(document(head)));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("create offer head", exception);
        }
    }

    private boolean replace(StoredHead current, OfferHead head) {
        try {
            client.index(index -> index.index(WRITE_ALIAS).id(current.id()).requireAlias(true).ifSeqNo(current.seqNo())
                    .ifPrimaryTerm(current.primaryTerm()).refresh(Refresh.WaitFor).document(document(head)));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("replace offer head", exception);
        }
    }

    private StoredHead stored(String id) {
        try {
            GetResponse<Map> response = client.get(get -> get.index(READ_ALIAS).id(id), Map.class);
            return response.found() ? stored(id, response.source(), response.seqNo(), response.primaryTerm()) : null;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 404) {
                return null;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("read offer head", exception);
        }
    }

    private StoredHead stored(Hit<Map> hit) {
        return stored(hit.id(), hit.source(), hit.seqNo(), hit.primaryTerm());
    }

    private StoredHead stored(String id, Map source, long seqNo, long primaryTerm) {
        try {
            OfferHead head = DataReferenceJson.mapper().readValue((String) source.get("headJson"), OfferHead.class);
            return new StoredHead(id, head, ((Number) source.get("revision")).longValue(), seqNo, primaryTerm);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("stored offer head violates its serialization contract", exception);
        }
    }

    private Map<String, Object> document(OfferHead head) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            OfferKey key = head.observation().key();
            document.put("offerKey", key.externalForm());
            document.put("gtin", key.gtin().value());
            document.put("providerId", key.providerId().value());
            document.put("providerOfferId", key.providerOfferId());
            document.put("revision", head.revision());
            document.put("headJson", DataReferenceJson.mapper().writeValueAsString(head));
            return document;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("offer head violates its serialization contract", exception);
        }
    }

    private synchronized void ensureIndex() {
        if (indexReady) {
            return;
        }
        try {
            if (!client.indices().exists(exists -> exists.index(READ_ALIAS)).value()) {
                client.indices().create(create -> create.index(READ_ALIAS + "-" + VERSION + "-000001")
                        .mappings(mapping()).aliases(READ_ALIAS, alias -> alias)
                        .aliases(WRITE_ALIAS, alias -> alias.isWriteIndex(true)));
            }
            indexReady = true;
        } catch (ElasticsearchException exception) {
            if (exception.status() != 400) {
                throw exception;
            }
            indexReady = true;
        } catch (IOException exception) {
            throw storageFailure("create offer-head aliases", exception);
        }
    }

    private TypeMapping mapping() {
        return new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("offerKey", property -> property.keyword(keyword -> keyword))
                .properties("gtin", property -> property.keyword(keyword -> keyword))
                .properties("providerId", property -> property.keyword(keyword -> keyword))
                .properties("providerOfferId", property -> property.keyword(keyword -> keyword))
                .properties("revision", property -> property.long_(number -> number))
                .properties("headJson", property -> property.keyword(keyword -> keyword.index(false).docValues(false)))
                .build();
    }

    private void closePit(String pitId) {
        try {
            ClosePointInTimeResponse response = client.closePointInTime(close -> close.id(pitId));
            if (!response.succeeded()) {
                throw new IllegalStateException("Elasticsearch did not acknowledge close of an offer-head provider scan");
            }
        } catch (ElasticsearchException exception) {
            if (exception.status() != 404) {
                throw exception;
            }
        } catch (IOException exception) {
            throw storageFailure("close offer-head provider scan", exception);
        }
    }

    private String idFor(OfferKey key) {
        try {
            return "offer-head:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.externalForm().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private IllegalStateException storageFailure(String action, IOException exception) {
        return new IllegalStateException("could not " + action, exception);
    }

    private record StoredHead(String id, OfferHead head, long revision, long seqNo, long primaryTerm) {
    }

    /**
     * Lazily pages one provider's current heads through a single point-in-time snapshot, so a
     * complete-feed reconciliation never materializes the provider's whole catalogue at once.
     */
    private final class ProviderHeadIterator implements Iterator<OfferHead> {

        private final String pitId;
        private final SourceId providerId;
        private Iterator<Hit<Map>> page = List.<Hit<Map>>of().iterator();
        private String lastSort;
        private boolean exhausted;

        private ProviderHeadIterator(String pitId, SourceId providerId) {
            this.pitId = pitId;
            this.providerId = providerId;
        }

        String pitId() {
            return pitId;
        }

        @Override
        public boolean hasNext() {
            if (page.hasNext()) {
                return true;
            }
            if (exhausted) {
                return false;
            }
            fetchNextPage();
            return page.hasNext();
        }

        @Override
        public OfferHead next() {
            if (!hasNext()) {
                throw new NoSuchElementException("offer-head provider scan has no further elements");
            }
            return stored(page.next()).head();
        }

        private void fetchNextPage() {
            try {
                String searchAfter = lastSort;
                var response = client.search(search -> {
                    search.pit(pit -> pit.id(pitId).keepAlive(keepAlive -> keepAlive.time(PIT_KEEP_ALIVE)))
                            .size(PROVIDER_SCAN_PAGE_SIZE).seqNoPrimaryTerm(true)
                            .query(query -> query.term(term -> term.field("providerId").value(providerId.value())))
                            .sort(sort -> sort.field(field -> field.field("offerKey").order(SortOrder.Asc)));
                    if (searchAfter != null) {
                        search.searchAfter(searchAfter);
                    }
                    return search;
                }, Map.class);
                List<Hit<Map>> hits = response.hits().hits();
                page = hits.iterator();
                if (hits.isEmpty()) {
                    exhausted = true;
                    return;
                }
                lastSort = (String) hits.getLast().source().get("offerKey");
            } catch (IOException exception) {
                throw storageFailure("scan offer heads by provider", exception);
            }
        }
    }
}

package org.open4goods.api.repository.datareference;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.pricehistory.model.LegacyPriceBackfill;
import org.open4goods.pricehistory.port.LegacyPriceBackfillStore;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;

/**
 * Elasticsearch store for neutral legacy Product minimum-price backfills.
 *
 * <p>One document per deterministic {@link LegacyPriceBackfill#id()}; a create with that id is
 * the store's idempotence mechanism, so a replay of the same legacy point never rewrites or
 * duplicates it (AC5). This index is deliberately separate from the live offer/price-event
 * pipeline: it is excluded from public queries by default (see {@link LegacyPriceBackfill}).
 */
@Repository
public class ElasticsearchLegacyPriceBackfillStore implements LegacyPriceBackfillStore {

    /** Read alias for legacy price backfills. */
    public static final String READ_ALIAS = "o4g-legacy-price-backfill-read";
    /** Write alias for legacy price backfills. */
    public static final String WRITE_ALIAS = "o4g-legacy-price-backfill-write";

    private static final String VERSION = "v1";

    private final ElasticsearchClient client;
    private volatile boolean indexReady;

    public ElasticsearchLegacyPriceBackfillStore(ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public boolean append(LegacyPriceBackfill backfill) {
        Objects.requireNonNull(backfill, "backfill must not be null");
        ensureIndex();
        try {
            client.index(index -> index.index(WRITE_ALIAS).id(backfill.id()).opType(OpType.Create).requireAlias(true)
                    .refresh(Refresh.WaitFor).document(document(backfill)));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("could not append legacy price backfill", exception);
        }
    }

    private Map<String, Object> document(LegacyPriceBackfill backfill) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("gtin", backfill.point().gtin().value());
            document.put("backfillJson", DataReferenceJson.mapper().writeValueAsString(backfill));
            return document;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("legacy price backfill violates its serialization contract", exception);
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
        } catch (IOException exception) {
            throw new IllegalStateException("could not create legacy price backfill aliases", exception);
        }
    }

    private TypeMapping mapping() {
        return new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("gtin", property -> property.keyword(keyword -> keyword))
                .properties("backfillJson", property -> property.keyword(keyword -> keyword.index(false).docValues(false)))
                .build();
    }
}

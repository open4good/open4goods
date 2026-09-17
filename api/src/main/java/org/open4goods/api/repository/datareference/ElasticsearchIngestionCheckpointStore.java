package org.open4goods.api.repository.datareference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.GetResponse;

/**
 * Elasticsearch store for importer-owned progress.
 *
 * <p>Checkpoints deliberately have their own index and deterministic identity. They cannot be
 * mistaken for product source heads, legacy datasource coordinates, or price events.
 */
@Repository
public class ElasticsearchIngestionCheckpointStore implements IngestionCheckpointStore {

    /** Read alias for importer-owned checkpoints. */
    public static final String READ_ALIAS = "o4g-ingestion-checkpoints-read";
    /** Write alias for importer-owned checkpoints. */
    public static final String WRITE_ALIAS = "o4g-ingestion-checkpoints-write";

    private static final String VERSION = "v1";

    private final ElasticsearchClient client;
    private volatile boolean indexReady;

    /**
     * Creates the checkpoint persistence adapter.
     *
     * @param client configured Elasticsearch client
     */
    public ElasticsearchIngestionCheckpointStore(ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Optional<IngestionCheckpoint> find(String owner, SourceId sourceId) {
        validateIdentity(owner, sourceId);
        ensureIndex();
        try {
            GetResponse<Map> response = client.get(get -> get.index(READ_ALIAS).id(idFor(owner, sourceId)), Map.class);
            if (!response.found()) {
                return Optional.empty();
            }
            IngestionCheckpoint checkpoint = DataReferenceJson.mapper().readValue(
                    (String) response.source().get("checkpointJson"), IngestionCheckpoint.class);
            return Optional.of(new IngestionCheckpoint(checkpoint.owner(), checkpoint.sourceId(), checkpoint.cursor(),
                    checkpoint.retryCount(), checkpoint.nextRetryAt(), checkpoint.updatedAt(),
                    ((Number) response.source().get("revision")).longValue()));
        } catch (IOException exception) {
            throw storageFailure("read ingestion checkpoint", exception);
        }
    }

    @Override
    public boolean compareAndSet(IngestionCheckpoint checkpoint, long expectedRevision) {
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must not be negative");
        }
        ensureIndex();
        String id = idFor(checkpoint.owner(), checkpoint.sourceId());
        StoredCheckpoint current = stored(id);
        if (current == null) {
            if (expectedRevision != 0) {
                return false;
            }
            return create(id, checkpoint);
        }
        if (current.revision() != expectedRevision) {
            return false;
        }
        return replace(current, checkpoint);
    }

    private boolean create(String id, IngestionCheckpoint checkpoint) {
        try {
            client.index(index -> index.index(WRITE_ALIAS).id(id).opType(OpType.Create).requireAlias(true)
                    .refresh(Refresh.WaitFor).document(document(checkpoint, 1)));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("create ingestion checkpoint", exception);
        }
    }

    private boolean replace(StoredCheckpoint current, IngestionCheckpoint checkpoint) {
        try {
            client.index(index -> index.index(WRITE_ALIAS).id(current.id()).requireAlias(true).ifSeqNo(current.seqNo())
                    .ifPrimaryTerm(current.primaryTerm()).refresh(Refresh.WaitFor)
                    .document(document(checkpoint, current.revision() + 1)));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("replace ingestion checkpoint", exception);
        }
    }

    private StoredCheckpoint stored(String id) {
        try {
            GetResponse<Map> response = client.get(get -> get.index(READ_ALIAS).id(id), Map.class);
            return response.found()
                    ? new StoredCheckpoint(id, ((Number) response.source().get("revision")).longValue(), response.seqNo(),
                            response.primaryTerm())
                    : null;
        } catch (IOException exception) {
            throw storageFailure("read ingestion checkpoint revision", exception);
        }
    }

    private Map<String, Object> document(IngestionCheckpoint checkpoint, long revision) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("owner", checkpoint.owner());
            document.put("sourceId", checkpoint.sourceId().value());
            document.put("revision", revision);
            document.put("checkpointJson", DataReferenceJson.mapper().writeValueAsString(checkpoint));
            return document;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("ingestion checkpoint violates its serialization contract", exception);
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
            throw storageFailure("create ingestion checkpoint aliases", exception);
        }
    }

    private TypeMapping mapping() {
        return new TypeMapping.Builder().dynamic(DynamicMapping.Strict)
                .properties("owner", property -> property.keyword(keyword -> keyword))
                .properties("sourceId", property -> property.keyword(keyword -> keyword))
                .properties("revision", property -> property.long_(number -> number))
                .properties("checkpointJson", property -> property.keyword(keyword -> keyword.index(false).docValues(false)))
                .build();
    }

    private String idFor(String owner, SourceId sourceId) {
        validateIdentity(owner, sourceId);
        try {
            String input = owner + "\u0000" + sourceId.value();
            return "ingestion-checkpoint:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private void validateIdentity(String owner, SourceId sourceId) {
        if (owner == null || !owner.matches("[a-z0-9][a-z0-9._-]{0,119}")) {
            throw new IllegalArgumentException("owner must be a lower-case bounded identifier");
        }
        Objects.requireNonNull(sourceId, "sourceId must not be null");
    }

    private IllegalStateException storageFailure(String action, IOException exception) {
        return new IllegalStateException("could not " + action, exception);
    }

    private record StoredCheckpoint(String id, long revision, long seqNo, long primaryTerm) {
    }
}

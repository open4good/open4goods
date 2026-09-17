package org.open4goods.services.eprelservice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.ScanCursor;
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Retrieves EPREL catalogues and writes source-record heads.
 *
 * <p>This service is the EPREL source boundary. In particular, it does not index
 * {@code EprelProduct} documents or mutate legacy product documents. Each accepted catalogue row
 * is a {@code FULL} source-record replacement, so a later row can remove an assertion that was
 * present in an earlier version of the same EPREL registration.
 */
@Service
public class EprelCatalogueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EprelCatalogueService.class);
    private static final String SCHEMA_VERSION = "catalogue-v1";
    private static final String CHECKPOINT_OWNER = "eprel-catalogue-v1";

    private final EprelApiClient apiClient;
    private final EprelCatalogueParser parser;
    private final SourceRecordHeadStore sourceRecordStore;
    private final IngestionCheckpointStore checkpointStore;
    private final EprelSourceRecordAdapter adapter;
    private final EprelServiceProperties properties;
    private final MeterRegistry meterRegistry;

    /**
     * Creates the EPREL catalogue importer.
     *
     * @param apiClient HTTP client used to interact with EPREL
     * @param parser parser converting catalogues to provider rows
     * @param sourceRecordStore persistent source-record boundary
     * @param checkpointStore durable importer progress boundary
     * @param adapter mapper that preserves provider field identities
     * @param properties module configuration
     * @param meterRegistry application metrics registry
     */
    public EprelCatalogueService(
            EprelApiClient apiClient,
            EprelCatalogueParser parser,
            SourceRecordHeadStore sourceRecordStore,
            IngestionCheckpointStore checkpointStore,
            EprelSourceRecordAdapter adapter,
            EprelServiceProperties properties,
            MeterRegistry meterRegistry) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.sourceRecordStore = Objects.requireNonNull(sourceRecordStore, "sourceRecordStore must not be null");
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore must not be null");
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    /**
     * Downloads every configured EPREL catalogue and applies its source-record rows.
     *
     * @throws IOException when EPREL cannot provide a complete configured catalogue
     */
    public void refreshCatalogue() throws IOException {
        List<EprelProductGroup> groups = apiClient.fetchProductGroups();
        if (groups.isEmpty()) {
            LOGGER.warn("No EPREL product groups returned by the API");
            return;
        }

        List<String> groupsToIndex = properties.getGroupsToIndex();
        for (EprelProductGroup group : groups) {
            if (groupsToIndex != null && !groupsToIndex.isEmpty() && !groupsToIndex.contains(group.urlCode())) {
                LOGGER.info("Skipping EPREL group {}", group.urlCode());
                continue;
            }
            processGroup(group);
        }
    }

    /**
     * Returns catalogue groups advertised by EPREL.
     *
     * @return current advertised product groups
     */
    public List<EprelProductGroup> getCatalog() {
        return apiClient.fetchProductGroups();
    }

    private void processGroup(EprelProductGroup group) throws IOException {
        Path zipPath = null;
        try {
            LOGGER.info("Downloading EPREL catalogue for {}", group.urlCode());
            zipPath = apiClient.downloadCatalogueZip(group.urlCode());
            Instant retrievedAt = Instant.now();
            List<SourceRecordMutation> buffer = new ArrayList<>(properties.getIndexBulkSize());
            parser.parse(zipPath, product -> adapter.adapt(product, SCHEMA_VERSION, retrievedAt).ifPresentOrElse(
                    buffer::add,
                    () -> increment("rejected")));
            // Do not publish any FULL replacement until the parser has verified the complete
            // catalogue. A parser failure must leave the preceding source heads untouched;
            // otherwise assertions absent from an interrupted catalogue could look like a
            // successful, partial refresh.
            flush(buffer);
            acknowledgeCompletedCatalogue(group, retrievedAt);
        } catch (IOException exception) {
            LOGGER.error("Failed to process EPREL catalogue for {}", group.urlCode(), exception);
            throw exception;
        } finally {
            deleteTemporaryArchive(zipPath);
        }
    }

    private void flush(List<SourceRecordMutation> buffer) {
        for (SourceRecordMutation mutation : buffer) {
            var transition = sourceRecordStore.apply(mutation);
            if (!mutation.candidate().gtinLinks().isEmpty()) {
                increment("mapped");
            } else {
                increment("unmapped");
            }
            if (mutation.candidate().state() == SourceRecordState.DELETED) {
                increment("withdrawn");
            }
            if (transition.outcome() == SourceRecordTransitionOutcome.ACCEPTED
                    || transition.outcome() == SourceRecordTransitionOutcome.TOMBSTONED) {
                increment("changed");
            }
        }
        buffer.clear();
    }

    private void increment(String outcome) {
        meterRegistry.counter("o4g.eprel.catalogue.records", "outcome", outcome).increment();
    }

    private void acknowledgeCompletedCatalogue(EprelProductGroup group, Instant completedAt) {
        SourceId sourceId = new SourceId(EprelSourceRecordAdapter.SOURCE_ID);
        // The checkpoint moves only after every parsed row has reached the source-head store. A
        // failed catalogue is therefore replayed from its beginning; record hashes make that
        // replay idempotent and no interrupted row can become a partial FULL head.
        for (int attempts = 0; attempts < 8; attempts++) {
            Optional<IngestionCheckpoint> current = checkpointStore.find(CHECKPOINT_OWNER, sourceId);
            IngestionCheckpoint next = new IngestionCheckpoint(
                    CHECKPOINT_OWNER,
                    sourceId,
                    Optional.of(new ScanCursor(SCHEMA_VERSION + ":" + group.urlCode())),
                    0,
                    Optional.empty(),
                    completedAt,
                    current.map(IngestionCheckpoint::revision).orElse(0L) + 1);
            if (checkpointStore.compareAndSet(next, current.map(IngestionCheckpoint::revision).orElse(0L))) {
                return;
            }
        }
        throw new IllegalStateException("EPREL catalogue checkpoint remained concurrently modified after 8 retries");
    }

    private void deleteTemporaryArchive(Path zipPath) {
        if (zipPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(zipPath);
        } catch (IOException exception) {
            LOGGER.warn("Unable to delete temporary EPREL catalogue {}", zipPath, exception);
        }
    }
}

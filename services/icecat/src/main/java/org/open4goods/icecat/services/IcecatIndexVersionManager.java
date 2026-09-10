package org.open4goods.icecat.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Builds a new, uniquely-named physical Elasticsearch index for each Icecat reference-data
 * import, validates its document count, and atomically switches the entity's configured
 * index name (an alias, not a concrete index — see {@code createIndex = false} on the
 * {@code @Document}-annotated classes) to point at it.
 *
 * <p>The previously-live index is retained (not deleted) so a bad switch can be rolled back;
 * only versions beyond {@link #RETAINED_VERSIONS} are pruned. On any failure before the alias
 * switch, the alias is left untouched and the partially-built new index is removed — a failed
 * import never leaves the read path in an inconsistent state, and can simply be retried.
 */
public class IcecatIndexVersionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatIndexVersionManager.class);

    /** Number of index versions kept per alias (the live one plus this many for rollback). */
    static final int RETAINED_VERSIONS = 2;

    private final ElasticsearchOperations operations;

    public IcecatIndexVersionManager(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    /**
     * Result of a successful {@link #reimport}.
     *
     * @param indexName      the new physical index now live behind the alias
     * @param documentCount  documents written and validated
     * @param previousIndexes indexes that backed the alias immediately before this switch
     */
    public record IndexSwitchResult(String indexName, long documentCount, Set<String> previousIndexes) {
    }

    /**
     * Imports {@code docs} into a new versioned index for {@code documentClass}, validates the
     * count, then atomically switches the class's configured alias to it.
     *
     * @param documentClass the {@code @Document}-annotated entity type (its configured index
     *                      name is treated as an alias, never written to directly)
     * @param docs          documents to write into the new index version
     * @return the switch result
     * @throws IllegalStateException if validation or the alias switch fails; the alias is left
     *                                untouched and the partial index is deleted before throwing
     */
    public <T> IndexSwitchResult reimport(Class<T> documentClass, List<T> docs) {
        IndexOperations entityIndexOps = operations.indexOps(documentClass);
        String aliasName = entityIndexOps.getIndexCoordinates().getIndexName();

        migrateConcreteIndexIfPresent(aliasName);

        String newIndexName = aliasName + "-" + Instant.now().toEpochMilli();
        IndexCoordinates newCoords = IndexCoordinates.of(newIndexName);
        IndexOperations newIndexOps = operations.indexOps(newCoords);

        try {
            newIndexOps.create(new HashMap<>(entityIndexOps.createSettings()), entityIndexOps.createMapping());

            if (!docs.isEmpty()) {
                operations.save(docs, newCoords);
            }
            // Elasticsearch is near-real-time: a write is not visible to count()/search() until
            // the next refresh, which does not happen automatically within this method's timeframe.
            newIndexOps.refresh();

            long expected = docs.size();
            long actual = operations.count(Query.findAll(), documentClass, newCoords);
            if (actual != expected) {
                throw new IllegalStateException(
                        "Index validation failed for %s: expected %d documents, indexed %d"
                                .formatted(aliasName, expected, actual));
            }

            Set<String> previousIndexes = currentIndexesForAlias(aliasName);
            switchAlias(newIndexOps, aliasName, newIndexName, previousIndexes);
            pruneOldIndexes(aliasName, previousIndexes, newIndexName);

            LOGGER.info("Icecat index {} switched to {} ({} documents), retaining up to {} previous version(s)",
                    aliasName, newIndexName, actual, RETAINED_VERSIONS - 1);
            return new IndexSwitchResult(newIndexName, actual, previousIndexes);
        } catch (RuntimeException e) {
            LOGGER.error("Import failed for {}, deleting partial index {} and leaving the alias untouched",
                    aliasName, newIndexName, e);
            if (newIndexOps.exists()) {
                newIndexOps.delete();
            }
            throw e;
        }
    }

    /**
     * Points {@code aliasName} back at a previously-retained index version.
     *
     * @param aliasName       the entity's configured (aliased) index name
     * @param targetIndexName a physical index name previously returned by {@link #reimport} for
     *                        this alias, still retained (not yet pruned)
     * @throws IllegalStateException if the target index no longer exists or the switch fails
     */
    public void rollback(String aliasName, String targetIndexName) {
        IndexOperations targetOps = operations.indexOps(IndexCoordinates.of(targetIndexName));
        if (!targetOps.exists()) {
            throw new IllegalStateException(
                    "Cannot roll back " + aliasName + " to " + targetIndexName + ": index no longer exists");
        }
        Set<String> current = currentIndexesForAlias(aliasName);
        switchAlias(targetOps, aliasName, targetIndexName, current);
        LOGGER.info("Rolled back {} to {}", aliasName, targetIndexName);
    }

    /**
     * Physical indexes currently backing {@code aliasName}, for admin/health inspection.
     * Empty when {@code aliasName} does not exist yet (nothing has ever been imported) or exists
     * as a concrete, non-aliased index (see {@link #migrateConcreteIndexIfPresent}).
     */
    public Set<String> currentIndexesForAlias(String aliasName) {
        try {
            return operations.indexOps(IndexCoordinates.of(aliasName)).getAliases(aliasName).keySet();
        } catch (org.springframework.data.elasticsearch.ResourceNotFoundException e) {
            return Set.of();
        }
    }

    /**
     * A pre-existing deployment may still have {@code aliasName} as a concrete index (created by
     * the old {@code createIndex = true} mapping). An alias cannot share a name with a concrete
     * index, so that index is deleted here — its data is fully re-derivable from the next Icecat
     * bulk import, which runs immediately after this check in {@link #reimport}.
     */
    private void migrateConcreteIndexIfPresent(String aliasName) {
        IndexOperations aliasIndexOps = operations.indexOps(IndexCoordinates.of(aliasName));
        if (aliasIndexOps.exists() && currentIndexesForAlias(aliasName).isEmpty()) {
            LOGGER.warn("{} exists as a concrete index rather than an alias; deleting it so it can become "
                    + "version-managed. Its data is fully re-derivable from this import.", aliasName);
            aliasIndexOps.delete();
        }
    }

    private void switchAlias(IndexOperations newIndexOps, String aliasName, String newIndexName,
            Set<String> previousIndexes) {
        AliasActions actions = new AliasActions();
        actions.add(new AliasAction.Add(
                AliasActionParameters.builder().withIndices(newIndexName).withAliases(aliasName).build()));
        for (String previous : previousIndexes) {
            if (!previous.equals(newIndexName)) {
                actions.add(new AliasAction.Remove(
                        AliasActionParameters.builder().withIndices(previous).withAliases(aliasName).build()));
            }
        }
        if (!newIndexOps.alias(actions)) {
            throw new IllegalStateException("Elasticsearch did not acknowledge the alias switch for " + aliasName);
        }
    }

    private void pruneOldIndexes(String aliasName, Set<String> previousIndexes, String newIndexName) {
        List<String> retained = new ArrayList<>(previousIndexes);
        retained.remove(newIndexName);
        // The version suffix is a millisecond timestamp, so lexical order is chronological order.
        retained.sort(Comparator.reverseOrder());
        for (int i = RETAINED_VERSIONS - 1; i < retained.size(); i++) {
            String stale = retained.get(i);
            LOGGER.info("Pruning stale Icecat index {} (beyond the {} retained versions of {})", stale,
                    RETAINED_VERSIONS, aliasName);
            operations.indexOps(IndexCoordinates.of(stale)).delete();
        }
    }
}

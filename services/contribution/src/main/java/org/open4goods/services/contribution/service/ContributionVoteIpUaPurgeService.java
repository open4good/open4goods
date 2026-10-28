package org.open4goods.services.contribution.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.services.contribution.model.ContributionVote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;

/**
 * One-shot, idempotent purge of the historical {@code ip} and {@code ua} fields of the
 * {@value ContributionVote#INDEX_NAME} index (board approval 2f4935d1, GOU-1222 / GOU-1230).
 * <p>
 * Only the documents holding an {@code ip} or a {@code ua} value are selected
 * ({@code bool.should [exists ip, exists ua]}, {@code minimum_should_match 1}); the update script removes these two
 * keys from {@code _source} and nothing else. No document is deleted. Version conflicts are skipped
 * ({@code conflicts=proceed}): a skipped document still matches the query and is purged by the next pass or the next
 * run.
 * <p>
 * Both fields are mapped {@code index: false} by {@link ContributionVote}: {@code exists} then relies on their doc
 * values (enabled by default for {@code ip} and {@code keyword}). The purge reads the live mapping first and refuses
 * to run when one of the fields could not be found by {@code exists}, instead of wrongly reporting nothing to purge.
 * <p>
 * The update runs in passes of at most {@value #BATCH_SIZE} documents ({@code max_docs}) so that no single request
 * outlives the client socket timeout; each pass refreshes the index, so the next one only sees the remaining
 * documents. The request is built with the native Elasticsearch client, and sent through the
 * {@link ElasticsearchTemplate} that front-api already uses, so that {@code conflicts}, {@code refresh} and the script
 * are exactly the ones written here.
 * <p>
 * Every log line starts with {@value #LOG_PREFIX} and holds counts only: never an IP, a User-Agent nor an
 * Elasticsearch error reason (which may quote a field value).
 */
public class ContributionVoteIpUaPurgeService {

    /** Stable prefix of every log line, for {@code grep}. */
    public static final String LOG_PREFIX = "contribution-votes purge ip/ua";

    /** Painless script removing the two personal fields, and only them, from {@code _source}. */
    public static final String SCRIPT = "ctx._source.remove(\"ip\"); ctx._source.remove(\"ua\")";

    /** Maximum number of documents updated by one {@code _update_by_query} pass. */
    public static final int BATCH_SIZE = 10_000;

    /** Hard stop on the number of passes (10 million documents), against any unforeseen loop. */
    static final int MAX_PASSES = 1_000;

    static final String FIELD_IP = "ip";
    static final String FIELD_UA = "ua";

    private static final Logger LOGGER = LoggerFactory.getLogger(ContributionVoteIpUaPurgeService.class);
    private static final int MAX_LOGGED_FAILURES = 10;
    private static final IndexCoordinates INDEX = IndexCoordinates.of(ContributionVote.INDEX_NAME);

    private final ElasticsearchOperations elasticsearchOperations;
    private final int batchSize;

    public ContributionVoteIpUaPurgeService(ElasticsearchOperations elasticsearchOperations) {
        this(elasticsearchOperations, BATCH_SIZE);
    }

    /**
     * @param elasticsearchOperations access to the {@value ContributionVote#INDEX_NAME} index
     * @param batchSize               maximum number of documents updated by one pass (tests use a small one)
     */
    ContributionVoteIpUaPurgeService(ElasticsearchOperations elasticsearchOperations, int batchSize) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.batchSize = batchSize;
    }

    /**
     * Outcome of a purge run.
     *
     * @param executed         whether an {@code _update_by_query} was sent
     * @param totalBefore      documents in the index before the purge
     * @param withIpOrUaBefore documents holding an {@code ip} or a {@code ua} before the purge
     * @param totalAfter       documents in the index after the purge (equals {@code totalBefore} when not executed)
     * @param withIpOrUaAfter  documents still holding an {@code ip} or a {@code ua} after the purge
     * @param updated          documents updated
     * @param versionConflicts documents skipped on a version conflict
     * @param failures         bulk and search failures reported by Elasticsearch
     * @param passes           number of {@code _update_by_query} requests sent
     */
    public record Report(boolean executed, long totalBefore, long withIpOrUaBefore, long totalAfter,
            long withIpOrUaAfter, long updated, long versionConflicts, long failures, int passes) {
    }

    /**
     * Runs the purge: counts, removes {@code ip}/{@code ua} from the matching documents, counts again, and logs both
     * sides at WARN. Does nothing, with a single log line, when the index is missing or no document holds one of the
     * fields.
     *
     * @return the counts of the run
     * @throws IllegalStateException when the live mapping does not allow {@code exists} on {@code ip} or {@code ua},
     *                               or when the operations are not an {@link ElasticsearchTemplate}
     * @throws RuntimeException      when Elasticsearch cannot answer (unreachable, authentication...); the documents
     *                               already updated stay purged and the next run resumes
     */
    public Report purge() {
        ElasticsearchTemplate template = template();
        IndexOperations indexOps = elasticsearchOperations.indexOps(INDEX);
        if (!indexOps.exists()) {
            LOGGER.warn("{} : rien à purger, index {} absent", LOG_PREFIX, ContributionVote.INDEX_NAME);
            return new Report(false, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        String mapping = checkMapping(indexOps.getMapping());

        long totalBefore = count(matchAll());
        long withBefore = count(withIpOrUa());
        if (withBefore == 0) {
            LOGGER.warn("{} : rien à purger (documents : {}, avec ip ou ua : 0 ; mapping {})", LOG_PREFIX,
                    totalBefore, mapping);
            return new Report(false, totalBefore, 0, totalBefore, 0, 0, 0, 0, 0);
        }
        LOGGER.warn("{} : avant purge, documents : {}, avec ip ou ua : {} ; mapping {}", LOG_PREFIX, totalBefore,
                withBefore, mapping);

        long updated = 0;
        long versionConflicts = 0;
        long failures = 0;
        int passes = 0;
        UpdateByQueryRequest request = updateByQueryRequest();
        while (passes < MAX_PASSES) {
            UpdateByQueryResponse response = template.execute(client -> client.updateByQuery(request));
            passes++;
            long passUpdated = orZero(response.updated());
            updated += passUpdated;
            versionConflicts += orZero(response.versionConflicts());
            failures += response.failures().size();
            logFailures(response);
            // A short pass means nothing is left (but skipped conflicts); a failed or timed out pass ends the loop
            // too. Whatever remains is counted and reported below.
            if (passUpdated < batchSize || !response.failures().isEmpty() || Boolean.TRUE.equals(response.timedOut())) {
                break;
            }
        }

        long totalAfter = count(matchAll());
        long withAfter = count(withIpOrUa());
        LOGGER.warn("{} : après purge, documents : {}, avec ip ou ua : {}, mis à jour : {}, conflits de version : {},"
                + " échecs : {}, passes : {}", LOG_PREFIX, totalAfter, withAfter, updated, versionConflicts, failures,
                passes);
        if (withAfter > 0 || failures > 0) {
            LOGGER.error("{} : purge incomplète, {} document(s) avec ip ou ua et {} échec(s) ; relancée au prochain"
                    + " démarrage", LOG_PREFIX, withAfter, failures);
        }
        return new Report(true, totalBefore, withBefore, totalAfter, withAfter, updated, versionConflicts, failures,
                passes);
    }

    /**
     * Builds one purge pass: {@code _update_by_query} on {@value ContributionVote#INDEX_NAME} with the
     * {@link #withIpOrUa()} query, the {@link #SCRIPT}, {@code conflicts=proceed}, {@code refresh=true} and at most
     * {@code batchSize} documents.
     *
     * @return the request sent for each pass
     */
    UpdateByQueryRequest updateByQueryRequest() {
        return UpdateByQueryRequest.of(u -> u
                .index(ContributionVote.INDEX_NAME)
                .query(withIpOrUa())
                .script(s -> s.source(src -> src.scriptString(SCRIPT)).lang("painless"))
                .conflicts(Conflicts.Proceed)
                .refresh(true)
                .maxDocs((long) batchSize));
    }

    private ElasticsearchTemplate template() {
        if (elasticsearchOperations instanceof ElasticsearchTemplate template) {
            return template;
        }
        throw new IllegalStateException("ElasticsearchTemplate attendu, reçu "
                + elasticsearchOperations.getClass().getName());
    }

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }

    /**
     * Documents holding a value for {@code ip} or for {@code ua}. A {@code null} value is not indexed, so a document
     * with {@code "ip": null} and no {@code ua} does not match: it holds no personal data.
     *
     * @return the selection query
     */
    public static Query withIpOrUa() {
        return Query.of(q -> q.bool(b -> b
                .should(s -> s.exists(e -> e.field(FIELD_IP)))
                .should(s -> s.exists(e -> e.field(FIELD_UA)))
                .minimumShouldMatch("1")));
    }

    private static Query matchAll() {
        return Query.of(q -> q.matchAll(m -> m));
    }

    private long count(Query query) {
        return elasticsearchOperations.count(NativeQuery.builder().withQuery(query).build(), INDEX);
    }

    /**
     * Checks that {@code exists} can see both fields in the live mapping and describes them (types and flags only).
     *
     * @param mapping the index mapping, as returned by {@link IndexOperations#getMapping()}
     * @return a one-line description of the {@code ip} and {@code ua} mappings, for the logs
     * @throws IllegalStateException when a field is neither indexed nor backed by doc values, or is unmapped while
     *                               dynamic mapping is disabled
     */
    static String checkMapping(Map<String, Object> mapping) {
        Object properties = mapping == null ? null : mapping.get("properties");
        Object dynamic = mapping == null ? null : mapping.get("dynamic");
        List<String> descriptions = new ArrayList<>();
        for (String field : List.of(FIELD_IP, FIELD_UA)) {
            Object fieldMapping = properties instanceof Map<?, ?> map ? map.get(field) : null;
            if (!(fieldMapping instanceof Map<?, ?> attributes)) {
                // Unmapped: with dynamic mapping on, no document has ever held a value for this field.
                if (dynamic != null && !"true".equals(String.valueOf(dynamic))) {
                    throw new IllegalStateException("champ " + field
                            + " absent du mapping avec dynamic=" + dynamic + ", exists ne peut pas le voir");
                }
                descriptions.add(field + "=non mappé");
                continue;
            }
            Object index = attributes.get("index");
            Object docValues = attributes.get("doc_values");
            if (isFalse(index) && isFalse(docValues)) {
                throw new IllegalStateException("champ " + field
                        + " ni indexé ni en doc_values, exists ne peut pas le voir");
            }
            descriptions.add(field + "={type=" + attributes.get("type") + ", index=" + (index == null ? "true" : index)
                    + ", doc_values=" + (docValues == null ? "true" : docValues) + "}");
        }
        return String.join(", ", descriptions);
    }

    private static boolean isFalse(Object flag) {
        return flag != null && "false".equals(String.valueOf(flag));
    }

    /**
     * Logs the first failures by index, document id and HTTP status: the error reason may quote a field value.
     */
    private static void logFailures(UpdateByQueryResponse response) {
        int logged = 0;
        for (BulkIndexByScrollFailure failure : response.failures()) {
            if (logged++ >= MAX_LOGGED_FAILURES) {
                break;
            }
            LOGGER.error("{} : échec sur le document {} (index {}, statut {}, type {})", LOG_PREFIX, failure.id(),
                    failure.index(), failure.status(), failure.cause() == null ? null : failure.cause().type());
        }
        if (Boolean.TRUE.equals(response.timedOut())) {
            LOGGER.error("{} : passe interrompue par un timeout", LOG_PREFIX);
        }
    }
}

package org.open4goods.services.contribution.service;

import java.util.Map;

import org.open4goods.services.contribution.model.AffiliationClick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

/**
 * Brings the mapping of an existing {@value AffiliationClick#INDEX_NAME} index up to date with
 * {@link AffiliationClick} (GOU-1828: {@code contentPath}).
 * <p>
 * {@code createIndex = true} only maps a <b>new</b> index: on an existing one, a new field would otherwise be mapped
 * dynamically on its first value ({@code text} plus a {@code .keyword} sub-field), which the aggregations cannot use.
 * The update is additive: Elasticsearch adds the missing fields and accepts the unchanged ones, so it is idempotent;
 * no document is rewritten (older documents simply have no value for the new fields). A field already mapped with
 * another type cannot be changed and makes the update fail.
 * <p>
 * Every log line starts with {@value #LOG_PREFIX}.
 */
public class AffiliationClickMappingService {

    /** Stable prefix of every log line, for {@code grep}. */
    public static final String LOG_PREFIX = "affiliation-clicks mapping";

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationClickMappingService.class);
    private static final String KEYWORD = "keyword";

    private final ElasticsearchOperations elasticsearchOperations;

    public AffiliationClickMappingService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /** What {@link #update()} did. */
    public enum Outcome {
        /** The index did not exist: it was created with the full mapping. */
        CREATED,
        /** The index existed: the missing fields were added, the others left unchanged. */
        UPDATED
    }

    /**
     * Creates the index with the {@link AffiliationClick} mapping when it is missing, or writes that mapping onto
     * the existing index, then checks that {@value AffiliationClick#FIELD_CONTENT_PATH} is a {@code keyword}.
     *
     * @return what was done
     * @throws IllegalStateException when the live mapping does not hold {@value AffiliationClick#FIELD_CONTENT_PATH}
     *                               as a {@code keyword}
     * @throws RuntimeException      when Elasticsearch cannot answer or refuses the mapping (a field already mapped
     *                               with another type)
     */
    public Outcome update() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(AffiliationClick.class);
        Outcome outcome;
        if (indexOps.exists()) {
            indexOps.putMapping();
            outcome = Outcome.UPDATED;
        } else {
            indexOps.createWithMapping();
            outcome = Outcome.CREATED;
        }
        String type = typeOf(indexOps.getMapping(), AffiliationClick.FIELD_CONTENT_PATH);
        if (!KEYWORD.equals(type)) {
            throw new IllegalStateException("champ " + AffiliationClick.FIELD_CONTENT_PATH + " de type " + type
                    + ", keyword attendu");
        }
        LOGGER.info("{} : index {} {}, {} en keyword", LOG_PREFIX, AffiliationClick.INDEX_NAME,
                outcome == Outcome.CREATED ? "créé" : "à jour", AffiliationClick.FIELD_CONTENT_PATH);
        return outcome;
    }

    /**
     * @return the {@code type} of {@code field} in {@code mapping}, {@code null} when the field is not mapped
     */
    static String typeOf(Map<String, Object> mapping, String field) {
        Object properties = mapping == null ? null : mapping.get("properties");
        Object fieldMapping = properties instanceof Map<?, ?> map ? map.get(field) : null;
        return fieldMapping instanceof Map<?, ?> attributes ? String.valueOf(attributes.get("type")) : null;
    }
}

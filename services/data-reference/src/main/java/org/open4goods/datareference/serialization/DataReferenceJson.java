package org.open4goods.datareference.serialization;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The one JSON configuration the product-reference contract is defined against.
 *
 * <p>A frozen discriminator is only frozen if every writer and reader agrees on
 * how to write it. Two consumers configuring their own mappers is how a contract
 * quietly acquires two encodings: one writing instants as epoch numbers and the
 * other as ISO strings, both "working" until a document written by one is read
 * by the other.
 *
 * <p>Unknown properties fail rather than being ignored. This contract is stored
 * data, so an unknown property means the reader is older than the document; a
 * reader that silently drops it produces a partially-read document and no error.
 */
public final class DataReferenceJson {

    private static final ObjectMapper MAPPER = build();

    private DataReferenceJson() {
    }

    /**
     * Returns the shared, immutable mapper for this contract.
     *
     * @return configured mapper
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Builds the canonical mapper configuration.
     *
     * @return configured mapper
     */
    private static ObjectMapper build() {
        return JsonMapper.builder()
                // Instants are ISO-8601 text: an epoch number is unreadable in a
                // stored document and loses the precision a reader needs to order
                // two observations. Stated explicitly rather than inherited, so a
                // Jackson default that changes cannot change stored documents.
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Collection and map order is part of the contract: assertion
                // ordinals and candidate ids are meaningful, so nothing may sort
                // them on the way out.
                .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
    }
}

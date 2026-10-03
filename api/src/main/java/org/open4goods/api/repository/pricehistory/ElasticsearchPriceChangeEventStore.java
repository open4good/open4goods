package org.open4goods.api.repository.pricehistory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.port.PriceChangeEventStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Elasticsearch persistence adapter for the immutable {@code PriceChangeEvent} time-series data
 * stream described by ADR-0011.
 *
 * <p>Reuses, unchanged, the index template and ILM policy classpath resources shipped by
 * {@code services/price-history} (also used read-only by {@code b2b-api}'s
 * {@code EsPriceHistoryQueryAdapter}), so this writer and that reader agree on exactly one schema.
 * A time-series data stream computes its document {@code _id} itself from the mapped dimensions
 * ({@code gtin}, {@code provider_id}, {@code provider_offer_id}, {@code condition},
 * {@code currency}) and {@code @timestamp}, and always rejects a client-supplied id; appending the
 * same logical event twice (same dimensions and {@link PriceChangeEvent#persistenceTimestamp()})
 * therefore surfaces as a 409 version conflict, which is this store's idempotence mechanism.
 */
@Repository
public class ElasticsearchPriceChangeEventStore implements PriceChangeEventStore {

    /** Concrete data stream name, matching the {@code o4g-price-change-*} template pattern. */
    public static final String DATA_STREAM = "o4g-price-change-events";

    private static final String TEMPLATE_NAME = "o4g-price-change";
    private static final String TEMPLATE_RESOURCE = "elasticsearch/price-change-index-template.json";
    private static final String ILM_POLICIES_RESOURCE = "elasticsearch/price-history-ilm-policies.json";
    private static final String ILM_POLICY_NAME = "o4g-price-change-24m";

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean dataStreamReady;

    /**
     * Creates the price-change-event persistence adapter.
     *
     * @param client configured Elasticsearch client
     */
    public ElasticsearchPriceChangeEventStore(ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public boolean append(PriceChangeEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        ensureDataStream();
        try {
            client.index(index -> index.index(DATA_STREAM).document(document(event)));
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return false;
            }
            throw exception;
        } catch (IOException exception) {
            throw storageFailure("append price change event", exception);
        }
    }

    private Map<String, Object> document(PriceChangeEvent event) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("event_id", event.id());
        document.put("gtin", event.key().gtin().value());
        document.put("provider_id", event.key().providerId().value());
        document.put("provider_offer_id", event.key().providerOfferId());
        document.put("condition", event.condition().name());
        document.put("currency", event.currency().getCurrencyCode());
        document.put("amount", event.amount().doubleValue());
        document.put("availability", event.availability().name());
        document.put("event_kind", event.kind().name());
        document.put("observed_at", event.observedAt().toString());
        document.put("policy_ref", event.policyRef().policyId() + ":" + event.policyRef().version());
        document.put("content_hash", event.contentHash().algorithm() + ":" + event.contentHash().hexadecimalValue());
        document.put("@timestamp", event.persistenceTimestamp().toString());
        return document;
    }

    /**
     * Idempotently provisions the ILM policy, index template and data stream on first use.
     *
     * <p>Mirrors the lazy {@code ensureIndex()} convention used by the data-reference stores
     * rather than {@code b2b-api}'s {@code ApplicationReadyEvent} provisioner: this store and that
     * reader run in separate deployable applications sharing one Elasticsearch cluster, so each
     * idempotently converges the identical desired state instead of one depending on the other's
     * startup order.
     */
    private synchronized void ensureDataStream() {
        if (dataStreamReady) {
            return;
        }
        putIlmPolicy();
        putIndexTemplate();
        createDataStream();
        dataStreamReady = true;
    }

    private void putIlmPolicy() {
        JsonNode policies = readJson(ILM_POLICIES_RESOURCE);
        JsonNode policy = policies.get(ILM_POLICY_NAME);
        if (policy == null) {
            throw new IllegalStateException("ILM policy " + ILM_POLICY_NAME + " is missing from " + ILM_POLICIES_RESOURCE);
        }
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.set("policy", policy);
        try (InputStream body = toInputStream(requestBody)) {
            client.ilm().putLifecycle(builder -> builder.name(ILM_POLICY_NAME).withJson(body));
        } catch (IOException | ElasticsearchException exception) {
            throw new IllegalStateException("could not register ILM policy " + ILM_POLICY_NAME, exception);
        }
    }

    private void putIndexTemplate() {
        try (InputStream body = new ClassPathResource(TEMPLATE_RESOURCE).getInputStream()) {
            client.indices().putIndexTemplate(builder -> builder.name(TEMPLATE_NAME).withJson(body));
        } catch (IOException | ElasticsearchException exception) {
            throw new IllegalStateException("could not register index template " + TEMPLATE_NAME, exception);
        }
    }

    private void createDataStream() {
        try {
            client.indices().createDataStream(create -> create.name(DATA_STREAM));
        } catch (ElasticsearchException exception) {
            if (exception.status() != 400) {
                throw exception;
            }
        } catch (IOException exception) {
            throw storageFailure("create price-change data stream", exception);
        }
    }

    private JsonNode readJson(String resourcePath) {
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException exception) {
            throw new IllegalStateException("could not read classpath resource " + resourcePath, exception);
        }
    }

    private InputStream toInputStream(JsonNode node) {
        try {
            return new ByteArrayInputStream(objectMapper.writeValueAsBytes(node));
        } catch (IOException exception) {
            throw new IllegalStateException("could not serialize ILM policy body", exception);
        }
    }

    private IllegalStateException storageFailure(String action, IOException exception) {
        return new IllegalStateException("could not " + action, exception);
    }
}

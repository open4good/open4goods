package org.open4goods.b2bapi.service.pricehistory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Idempotently provisions the price-history Elasticsearch schema (ILM policies, then index
 * templates) from the classpath resources shipped by the {@code price-history} module, on every
 * application startup.
 *
 * <p>Order matters: an index template's {@code index.lifecycle.name} setting is only validated at
 * rollover time, not at template registration time, but a template registered before its ILM
 * policy exists would reference a lifecycle that is never created if provisioning is interrupted
 * between the two steps. Policies are therefore always applied first.
 *
 * <p>This is the only writer for these three resources (GOU-144); the price-history facet itself
 * has no ingestion path yet (GOU-28 covers reads only), so a freshly provisioned environment sees
 * these data streams registered but empty until an ingestion writer exists.
 *
 * <p>Failures are logged, not thrown, so a local or test context that deliberately starts without
 * Elasticsearch still boots. {@link PriceHistoryProvisioningHealthIndicator} is the durable
 * guard-rail: it re-checks the live cluster state on every health probe, so a provisioning failure
 * or an out-of-band deletion is visible in {@code /actuator/health} rather than silently serving
 * the facet's empty-result envelope forever.
 */
@Component
public class PriceHistoryElasticsearchProvisioner {

    /** Name of the ILM policy backing the price-change data stream. */
    public static final String PRICE_CHANGE_POLICY = "o4g-price-change-24m";
    /** Name of the ILM policy backing the daily-provider-rollup data stream. */
    public static final String DAILY_ROLLUP_POLICY = "o4g-daily-provider-rollup-5y";
    /** Name of the index template backing the price-change data stream. */
    public static final String PRICE_CHANGE_TEMPLATE = "o4g-price-change";
    /** Name of the index template backing the daily-provider-rollup data stream. */
    public static final String DAILY_ROLLUP_TEMPLATE = "o4g-daily-provider-rollup";

    private static final String ILM_POLICIES_RESOURCE = "elasticsearch/price-history-ilm-policies.json";
    private static final String PRICE_CHANGE_TEMPLATE_RESOURCE = "elasticsearch/price-change-index-template.json";
    private static final String DAILY_ROLLUP_TEMPLATE_RESOURCE = "elasticsearch/daily-provider-rollup-index-template.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceHistoryElasticsearchProvisioner.class);

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;

    public PriceHistoryElasticsearchProvisioner(final ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Provisions the price-history schema once Spring context startup has finished, so a
     * transient Elasticsearch client wiring issue cannot prevent the application context itself
     * from coming up.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void provisionOnStartup() {
        try {
            provision();
            LOGGER.info("Price-history Elasticsearch schema provisioned: ILM policies [{}, {}], index templates [{}, {}]",
                    PRICE_CHANGE_POLICY, DAILY_ROLLUP_POLICY, PRICE_CHANGE_TEMPLATE, DAILY_ROLLUP_TEMPLATE);
        } catch (final RuntimeException exception) {
            LOGGER.error("Price-history Elasticsearch schema provisioning failed; the facet will keep serving "
                    + "empty results until this is fixed and the application is restarted or provisioning is "
                    + "retried: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Applies the ILM policies, then the index templates, from the classpath. Each {@code PUT} is
     * a full replace, so calling this repeatedly (every startup) is safe and keeps the live
     * cluster state aligned with the resources shipped in the running artifact.
     */
    public void provision() {
        putIlmPolicies();
        putIndexTemplate(PRICE_CHANGE_TEMPLATE, PRICE_CHANGE_TEMPLATE_RESOURCE);
        putIndexTemplate(DAILY_ROLLUP_TEMPLATE, DAILY_ROLLUP_TEMPLATE_RESOURCE);
    }

    private void putIlmPolicies() {
        final JsonNode policies = readJson(ILM_POLICIES_RESOURCE);
        final Iterator<Map.Entry<String, JsonNode>> fields = policies.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            putIlmPolicy(entry.getKey(), entry.getValue());
        }
    }

    private void putIlmPolicy(final String policyName, final JsonNode policyBody) {
        final ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.set("policy", policyBody);
        try (InputStream body = toInputStream(requestBody)) {
            client.ilm().putLifecycle(builder -> builder.name(policyName).withJson(body));
        } catch (final IOException | ElasticsearchException exception) {
            throw new PriceHistoryProvisioningException("Failed to register ILM policy " + policyName, exception);
        }
    }

    private void putIndexTemplate(final String templateName, final String resourcePath) {
        try (InputStream body = new ClassPathResource(resourcePath).getInputStream()) {
            client.indices().putIndexTemplate(builder -> builder.name(templateName).withJson(body));
        } catch (final IOException | ElasticsearchException exception) {
            throw new PriceHistoryProvisioningException("Failed to register index template " + templateName, exception);
        }
    }

    private JsonNode readJson(final String resourcePath) {
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (final IOException exception) {
            throw new PriceHistoryProvisioningException("Failed to read classpath resource " + resourcePath, exception);
        }
    }

    private InputStream toInputStream(final JsonNode node) {
        try {
            return new ByteArrayInputStream(objectMapper.writeValueAsBytes(node));
        } catch (final IOException exception) {
            throw new PriceHistoryProvisioningException("Failed to serialize ILM policy body", exception);
        }
    }

    /** Thrown when the price-history Elasticsearch schema could not be registered. */
    public static class PriceHistoryProvisioningException extends RuntimeException {
        public PriceHistoryProvisioningException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}

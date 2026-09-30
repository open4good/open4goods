package org.open4goods.b2bapi.service.pricehistory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;

/**
 * Guard-rail for GOU-144: fails {@code /actuator/health} when the price-history facet is served
 * without its Elasticsearch schema registered.
 *
 * <p>The price-history query adapter answers an empty {@code no-price-history} envelope (HTTP 200)
 * for any GTIN when its data streams do not exist yet - by design, so a wildcard index pattern
 * with no matching index is not an error (GOU-28). That makes a missing schema a silent failure
 * mode from the facet's own responses: everything looks correct while no data can ever be served.
 * This indicator re-checks the live cluster on every probe, independent of whether
 * {@link PriceHistoryElasticsearchProvisioner} ran successfully at this process's own startup, so
 * it also catches an out-of-band deletion of the templates or policies after a healthy start.
 */
@Component("priceHistoryProvisioning")
public class PriceHistoryProvisioningHealthIndicator implements HealthIndicator {

    private final ElasticsearchClient client;

    public PriceHistoryProvisioningHealthIndicator(final ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Health health() {
        final List<String> missing = new ArrayList<>();
        checkIndexTemplate(PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_TEMPLATE, missing);
        checkIndexTemplate(PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_TEMPLATE, missing);
        checkIlmPolicy(PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_POLICY, missing);
        checkIlmPolicy(PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_POLICY, missing);

        if (missing.isEmpty()) {
            return Health.up()
                    .withDetail("indexTemplates", List.of(
                            PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_TEMPLATE,
                            PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_TEMPLATE))
                    .withDetail("ilmPolicies", List.of(
                            PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_POLICY,
                            PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_POLICY))
                    .build();
        }
        return Health.down()
                .withDetail("missing", missing)
                .withDetail("reason",
                        "price-history facet is served without a full Elasticsearch schema; "
                                + "it will answer empty results for every GTIN until this is provisioned")
                .build();
    }

    private void checkIndexTemplate(final String name, final List<String> missing) {
        try {
            final boolean exists = client.indices().existsIndexTemplate(builder -> builder.name(name)).value();
            if (!exists) {
                missing.add("index-template:" + name);
            }
        } catch (final ElasticsearchException | IOException exception) {
            missing.add("index-template:" + name + " (check failed: " + exception.getMessage() + ")");
        }
    }

    private void checkIlmPolicy(final String name, final List<String> missing) {
        try {
            final boolean exists = !client.ilm().getLifecycle(builder -> builder.name(name)).lifecycles().isEmpty();
            if (!exists) {
                missing.add("ilm-policy:" + name);
            }
        } catch (final ElasticsearchException notFound) {
            missing.add("ilm-policy:" + name);
        } catch (final IOException exception) {
            missing.add("ilm-policy:" + name + " (check failed: " + exception.getMessage() + ")");
        }
    }
}

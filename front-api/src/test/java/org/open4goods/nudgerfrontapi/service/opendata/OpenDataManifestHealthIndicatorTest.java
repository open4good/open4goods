package org.open4goods.nudgerfrontapi.service.opendata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.OpenDataManifestProperties;
import org.open4goods.nudgerfrontapi.service.opendata.ManifestFixtures.MutableClock;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.springframework.boot.health.actuate.endpoint.SimpleStatusAggregator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Health component {@code openDataManifest} (GOU-2290): UNKNOWN without a base URL, DEGRADED when the manifest is
 * unreadable or a dataset is older than 8 days, and never a DOWN aggregate.
 */
class OpenDataManifestHealthIndicatorTest {

    private OpenDataConfig openDataConfig;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        openDataConfig = new OpenDataConfig();
        openDataConfig.setPublicBaseUrl("https://data.nudger.fr");
        clock = new MutableClock();
    }

    @Test
    void unknownWithoutBaseUrl() {
        openDataConfig.setPublicBaseUrl("");

        Health health = indicator(uri -> {
            throw new AssertionError("no HTTP call without a base URL");
        }).health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("configured", false);
    }

    @Test
    void upWhenEveryDatasetIsRecent() {
        Health health = indicator(uri -> ManifestFixtures.manifestAged(Duration.ofDays(7))).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("manifestUrl", "https://data.nudger.fr/manifest.json")
                .containsEntry("state", "AVAILABLE")
                .doesNotContainKey("problems");
        assertThat(health.getDetails().get("datasetAgeSeconds"))
                .asInstanceOf(InstanceOfAssertFactories.map(String.class, Long.class))
                .containsEntry("gtin", Duration.ofDays(7).toSeconds());
    }

    @Test
    void degradedBeyondEightDays() {
        Health health = indicator(uri -> ManifestFixtures.manifestAged(Duration.ofDays(8).plusMinutes(1))).health();

        assertThat(health.getStatus()).isEqualTo(OpenDataManifestHealthIndicator.DEGRADED);
        assertThat((List<?>) health.getDetails().get("problems")).asString()
                .contains("dataset gtin generated 8 days ago, more than 8");
    }

    @Test
    void degradedWhenTheManifestIsNotFound() {
        Health health = indicator(uri -> {
            throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);
        }).health();

        assertThat(health.getStatus()).isEqualTo(OpenDataManifestHealthIndicator.DEGRADED);
        assertThat(health.getDetails()).containsEntry("state", "UNAVAILABLE");
        assertThat((List<?>) health.getDetails().get("problems")).asString().contains("HTTP 404");
    }

    @Test
    void degradedWhenTheIsbnDatasetIsMissing() {
        String withoutIsbn = ManifestFixtures.manifestAged(Duration.ofDays(1)).replace("\"id\": \"isbn\"",
                "\"id\": \"other\"");

        Health health = indicator(uri -> withoutIsbn).health();

        assertThat(health.getStatus()).isEqualTo(OpenDataManifestHealthIndicator.DEGRADED);
        assertThat((List<?>) health.getDetails().get("problems")).asString()
                .contains("dataset isbn missing from the manifest");
    }

    @Test
    void degradedNeverTurnsTheAggregateDown() {
        SimpleStatusAggregator aggregator = new SimpleStatusAggregator();

        assertThat(aggregator.getAggregateStatus(Set.of(Status.UP, OpenDataManifestHealthIndicator.DEGRADED)))
                .isEqualTo(Status.UP);
        assertThat(aggregator.getAggregateStatus(Set.of(Status.UP, Status.UNKNOWN))).isEqualTo(Status.UP);
    }

    private OpenDataManifestHealthIndicator indicator(OpenDataManifestService.ManifestReader reader) {
        OpenDataManifestService service = new OpenDataManifestService(openDataConfig,
                new OpenDataManifestProperties(), reader, new SimpleMeterRegistry(), clock);
        return new OpenDataManifestHealthIndicator(service);
    }
}

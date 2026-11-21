package org.open4goods.nudgerfrontapi.service.opendata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.open4goods.nudgerfrontapi.service.opendata.ManifestFixtures.GTIN_BYTES;
import static org.open4goods.nudgerfrontapi.service.opendata.ManifestFixtures.GTIN_ROWS;
import static org.open4goods.nudgerfrontapi.service.opendata.ManifestFixtures.GTIN_SHA256;
import static org.open4goods.nudgerfrontapi.service.opendata.ManifestFixtures.NOW;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.OpenDataManifestProperties;
import org.open4goods.nudgerfrontapi.service.opendata.ManifestFixtures.MutableClock;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot.State;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Reading of the published {@code manifest.json} by {@link OpenDataManifestService} (GOU-2290): format of the api,
 * cache, failures, logs and age gauge.
 */
class OpenDataManifestServiceTest {

    private OpenDataConfig openDataConfig;
    private OpenDataManifestProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private MutableClock clock;
    private final List<URI> reads = new ArrayList<>();
    private ListAppender<ILoggingEvent> logs;
    private Logger serviceLogger;
    private Level previousLevel;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        openDataConfig = new OpenDataConfig();
        openDataConfig.setPublicBaseUrl("https://data.example/");
        properties = new OpenDataManifestProperties();
        meterRegistry = new SimpleMeterRegistry();
        clock = new MutableClock();
        serviceLogger = (Logger) LoggerFactory.getLogger(OpenDataManifestService.class);
        logs = new ListAppender<>();
        logs.start();
        previousLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(Level.DEBUG);
        serviceLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logs);
        serviceLogger.setLevel(previousLevel);
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void emptyBaseUrlNeverReadsTheManifest() {
        openDataConfig.setPublicBaseUrl("");
        OpenDataManifestService service = service(uri -> fail("no HTTP call without a base URL"));

        OpenDataManifestSnapshot snapshot = service.snapshot();

        assertThat(snapshot.state()).isEqualTo(State.NOT_CONFIGURED);
        assertThat(snapshot.dataset(OpenDataManifestService.GTIN)).isEmpty();
        assertThat(service.manifestUrl()).isNull();
        assertThat(ageGauge("gtin")).isNaN();
    }

    @Test
    void readsTheManifestOfTheApiAndIgnoresUnknownFields() {
        OpenDataManifestService service = service(recording(ManifestFixtures.manifestAged(Duration.ofDays(1))));

        OpenDataManifestSnapshot snapshot = service.snapshot();

        assertThat(reads).containsExactly(URI.create("https://data.example/manifest.json"));
        assertThat(snapshot.state()).isEqualTo(State.AVAILABLE);
        assertThat(snapshot.readAt()).isEqualTo(NOW);
        DatasetManifest gtin = snapshot.dataset(OpenDataManifestService.GTIN).orElseThrow();
        assertThat(gtin.rows()).isEqualTo(GTIN_ROWS);
        assertThat(gtin.bytes()).isEqualTo(GTIN_BYTES);
        assertThat(gtin.sha256()).isEqualTo(GTIN_SHA256);
        assertThat(snapshot.dataset("impact-tv")).isPresent();
    }

    @Test
    void readsOncePerCacheTtl() {
        OpenDataManifestService service = service(recording(ManifestFixtures.manifestAged(Duration.ofDays(1))));

        service.snapshot();
        clock.advance(Duration.ofMinutes(4));
        service.snapshot();
        assertThat(reads).hasSize(1);

        clock.advance(Duration.ofMinutes(1));
        service.snapshot();
        assertThat(reads).hasSize(2);
    }

    @Test
    void notFoundGivesNullValuesAndNoException() {
        OpenDataManifestService service = service(failing(HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                "Not Found", null, null, null)));

        OpenDataManifestSnapshot snapshot = service.snapshot();

        assertThat(snapshot.state()).isEqualTo(State.UNAVAILABLE);
        assertThat(snapshot.error()).isEqualTo("HTTP 404");
        assertThat(snapshot.manifest()).isNull();
        assertThat(snapshot.readAt()).isNull();
        assertThat(snapshot.dataset(OpenDataManifestService.GTIN)).isEmpty();
        assertThat(ageGauge("gtin")).isNaN();
    }

    @Test
    void invalidJsonGivesUnavailable() {
        OpenDataManifestService service = service(recording("<html>Not a manifest</html>"));

        OpenDataManifestSnapshot snapshot = service.snapshot();

        assertThat(snapshot.state()).isEqualTo(State.UNAVAILABLE);
        assertThat(snapshot.manifest()).isNull();
        assertThat(snapshot.error()).isNotBlank();
    }

    @Test
    void jsonWithoutDatasetsGivesUnavailable() {
        OpenDataManifestService service = service(recording("{\"schemaVersion\": 1}"));

        assertThat(service.snapshot().state()).isEqualTo(State.UNAVAILABLE);
        assertThat(service.snapshot().error()).contains("no datasets");
    }

    @Test
    void failureKeepsTheLastManifestAndIsCachedForTheTtl() {
        AtomicInteger calls = new AtomicInteger();
        String manifest = ManifestFixtures.manifestAged(Duration.ofDays(1));
        OpenDataManifestService service = service(uri -> {
            if (calls.incrementAndGet() == 1) {
                return manifest;
            }
            throw new ResourceAccessException("I/O error: Connection refused");
        });
        service.snapshot();

        clock.advance(Duration.ofMinutes(6));
        OpenDataManifestSnapshot failed = service.snapshot();
        assertThat(failed.state()).isEqualTo(State.UNAVAILABLE);
        assertThat(failed.readAt()).isEqualTo(NOW);
        assertThat(failed.dataset(OpenDataManifestService.GTIN)).map(DatasetManifest::rows).contains(GTIN_ROWS);

        clock.advance(Duration.ofMinutes(1));
        service.snapshot();
        assertThat(calls).hasValue(2);

        clock.advance(Duration.ofMinutes(5));
        service.snapshot();
        assertThat(calls).hasValue(3);
    }

    @Test
    void warnsOncePerReasonThenInfoOnRecovery() {
        AtomicInteger calls = new AtomicInteger();
        String manifest = ManifestFixtures.manifestAged(Duration.ofDays(1));
        OpenDataManifestService service = service(uri -> {
            if (calls.incrementAndGet() <= 3) {
                throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);
            }
            return manifest;
        });

        for (int i = 0; i < 4; i++) {
            service.snapshot();
            clock.advance(Duration.ofMinutes(6));
        }

        assertThat(logs.list).filteredOn(event -> event.getLevel() == Level.WARN).hasSize(1)
                .first().extracting(ILoggingEvent::getFormattedMessage).asString()
                .contains("https://data.example/manifest.json").contains("HTTP 404");
        assertThat(logs.list).filteredOn(event -> event.getLevel() == Level.INFO)
                .extracting(ILoggingEvent::getFormattedMessage).anyMatch(message -> message.contains("readable again"));
    }

    @Test
    void ageGaugePerDatasetOfTheManifest() {
        OpenDataManifestService service = service(recording(ManifestFixtures.manifestAged(Duration.ofDays(9))));
        service.snapshot();

        assertThat(ageGauge("gtin")).isEqualTo(Duration.ofDays(9).toSeconds());
        assertThat(ageGauge("isbn")).isEqualTo(Duration.ofDays(9).toSeconds());
        assertThat(ageGauge("impact-tv")).isEqualTo(Duration.ofDays(9).toSeconds());

        // The age keeps growing between two reads of the manifest.
        clock.advance(Duration.ofMinutes(2));
        assertThat(ageGauge("gtin")).isEqualTo(Duration.ofDays(9).plusMinutes(2).toSeconds());
    }

    @Test
    void invalidGeneratedAtGivesNoAgeNeverEpoch() {
        OpenDataManifestService service = service(recording(ManifestFixtures.manifest("not-a-date", "")));

        OpenDataManifestSnapshot snapshot = service.snapshot();

        assertThat(service.age(snapshot.dataset("gtin").orElseThrow())).isEmpty();
        assertThat(service.age(snapshot.dataset("isbn").orElseThrow())).isEmpty();
        assertThat(ageGauge("gtin")).isNaN();
    }

    @Test
    void realHttpReadsAndNotFound() throws IOException {
        String manifest = ManifestFixtures.manifestAged(Duration.ofDays(1));
        startServer(manifest, 0);
        OpenDataManifestService service = new OpenDataManifestService(openDataConfig, properties,
                OpenDataManifestService.restReader(RestClient.builder(), properties), meterRegistry, clock);

        openDataConfig.setPublicBaseUrl(baseUrl() + "/ok");
        assertThat(service.snapshot().state()).isEqualTo(State.AVAILABLE);
        assertThat(service.snapshot().dataset("gtin")).map(DatasetManifest::rows).contains(GTIN_ROWS);

        OpenDataManifestService missing = new OpenDataManifestService(openDataConfig, properties,
                OpenDataManifestService.restReader(RestClient.builder(), properties), new SimpleMeterRegistry(),
                clock);
        openDataConfig.setPublicBaseUrl(baseUrl() + "/missing");
        assertThat(missing.snapshot().state()).isEqualTo(State.UNAVAILABLE);
        assertThat(missing.snapshot().error()).isEqualTo("HTTP 404");
    }

    @Test
    void realHttpReadTimeoutIsShort() throws IOException {
        startServer(ManifestFixtures.manifestAged(Duration.ofDays(1)), 3000);
        properties.setReadTimeout(Duration.ofMillis(200));
        openDataConfig.setPublicBaseUrl(baseUrl() + "/ok");
        OpenDataManifestService service = new OpenDataManifestService(openDataConfig, properties,
                OpenDataManifestService.restReader(RestClient.builder(), properties), meterRegistry, clock);

        long start = System.nanoTime();
        OpenDataManifestSnapshot snapshot = service.snapshot();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(snapshot.state()).isEqualTo(State.UNAVAILABLE);
        assertThat(elapsedMillis).isLessThan(2500);
    }

    private OpenDataManifestService service(OpenDataManifestService.ManifestReader reader) {
        return new OpenDataManifestService(openDataConfig, properties, reader, meterRegistry, clock);
    }

    private OpenDataManifestService.ManifestReader recording(String body) {
        return uri -> {
            reads.add(uri);
            return body;
        };
    }

    private OpenDataManifestService.ManifestReader failing(RuntimeException exception) {
        return uri -> {
            reads.add(uri);
            throw exception;
        };
    }

    private double ageGauge(String dataset) {
        return meterRegistry.get(OpenDataManifestService.AGE_METRIC).tag("dataset", dataset).gauge().value();
    }

    private void startServer(String manifest, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok/manifest.json", exchange -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = manifest.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            } catch (IOException e) {
                // The client gave up (read timeout test).
            }
        });
        server.createContext("/missing/manifest.json", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}

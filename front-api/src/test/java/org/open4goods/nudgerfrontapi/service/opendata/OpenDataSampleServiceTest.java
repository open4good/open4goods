package org.open4goods.nudgerfrontapi.service.opendata;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.DatasetManifest;

class OpenDataSampleServiceTest {

    private static DatasetManifest dataset(String sample, String generatedAt) {
        return new DatasetManifest(1, "gtin", "gtin-open-data.zip", null, DatasetManifest.FORMAT, "ODbL-1.0", 1, "a", 2, 2,
                generatedAt, List.of(), sample);
    }

    private static OpenDataConfig config(String base) {
        OpenDataConfig config = new OpenDataConfig();
        config.setPublicBaseUrl(base);
        return config;
    }

    @Test
    void parsesTheRowsWithoutTheHeaderAndReadsOncePerGeneration() {
        AtomicInteger reads = new AtomicInteger();
        List<URI> uris = new ArrayList<>();
        OpenDataSampleService service = new OpenDataSampleService(config("https://data.nudger.fr/"), uri -> {
            reads.incrementAndGet();
            uris.add(uri);
            return "gtin,name\n123,\"a, b\"\n456,\n";
        }, Duration.ofMinutes(5), () -> 0L);

        Optional<DatasetManifest> entry = Optional.of(dataset("gtin-open-data-sample.csv", "2026-11-22T02:55:00Z"));
        assertThat(service.rows(entry)).containsExactly(List.of("123", "a, b"), List.of("456", ""));
        service.rows(entry);

        assertThat(reads).hasValue(1);
        assertThat(uris).containsExactly(URI.create("https://data.nudger.fr/gtin-open-data-sample.csv"));

        service.rows(Optional.of(dataset("gtin-open-data-sample.csv", "2026-11-23T02:55:00Z")));
        assertThat(reads).hasValue(2);
    }

    @Test
    void returnsNoRowsWithoutSampleBaseUrlOrOnFailure() {
        OpenDataSampleService failing = new OpenDataSampleService(config("https://data.nudger.fr"), uri -> {
            throw new IllegalStateException("404");
        }, Duration.ofMinutes(5), () -> 0L);
        assertThat(failing.rows(Optional.of(dataset("s.csv", "x")))).isEmpty();
        assertThat(failing.rows(Optional.of(dataset(null, "x")))).isEmpty();
        assertThat(failing.rows(Optional.empty())).isEmpty();
        assertThat(new OpenDataSampleService(config(""), uri -> "a\n1\n", Duration.ofMinutes(5), () -> 0L).rows(Optional.of(dataset("s.csv", "x"))))
                .isEmpty();
    }

    @Test
    void remembersAFailureUntilTheRetryDelayElapses() {
        AtomicInteger reads = new AtomicInteger();
        long[] now = {0L};
        OpenDataSampleService service = new OpenDataSampleService(config("https://data.nudger.fr"), uri -> {
            if (reads.incrementAndGet() == 1) {
                throw new IllegalStateException("timeout");
            }
            return "a\n1\n";
        }, Duration.ofMinutes(5), () -> now[0]);
        Optional<DatasetManifest> entry = Optional.of(dataset("s.csv", "x"));

        assertThat(service.rows(entry)).isEmpty();
        assertThat(service.rows(entry)).isEmpty();
        assertThat(reads).hasValue(1);

        now[0] = Duration.ofMinutes(5).toMillis();
        assertThat(service.rows(entry)).containsExactly(List.of("1"));
        assertThat(reads).hasValue(2);
    }
}

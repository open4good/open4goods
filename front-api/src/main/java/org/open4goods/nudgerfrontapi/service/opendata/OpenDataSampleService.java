package org.open4goods.nudgerfrontapi.service.opendata;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

import org.open4goods.nudgerfrontapi.config.properties.OpenDataManifestProperties;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Reads the CSV sample published next to a dataset (header and the first 20 rows, GOU-2744) so that the pages render
 * an extract server-side. The sample file name comes from the {@code sample} field of the dataset in
 * {@code manifest.json}; it only holds columns of the published dataset. A sample is read once per generation of the
 * dataset (key: file name and {@code generatedAt}); a read failure is logged once, remembered until
 * {@code manifestCacheTtl} has elapsed (no call per request) and the extract is simply absent.
 */
@Service
public class OpenDataSampleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataSampleService.class);

    private final OpenDataConfig openDataConfig;
    private final Function<URI, String> reader;
    private final Duration retryDelay;
    private final LongSupplier clock;
    private final Map<String, List<List<String>>> cache = new ConcurrentHashMap<>();
    /** Failed reads: key {@code sample@generatedAt} to the epoch millis after which a new read is allowed. */
    private final Map<String, Long> failures = new ConcurrentHashMap<>();

    @Autowired
    public OpenDataSampleService(OpenDataConfig openDataConfig, OpenDataManifestProperties properties,
            RestClient.Builder restClientBuilder) {
        this(openDataConfig, restReader(restClientBuilder, properties), properties.getManifestCacheTtl(),
                System::currentTimeMillis);
    }

    OpenDataSampleService(OpenDataConfig openDataConfig, Function<URI, String> reader, Duration retryDelay,
            LongSupplier clock) {
        this.openDataConfig = openDataConfig;
        this.reader = reader;
        this.retryDelay = retryDelay;
        this.clock = clock;
    }

    private static Function<URI, String> restReader(RestClient.Builder restClientBuilder,
            OpenDataManifestProperties properties) {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        RestClient restClient = restClientBuilder.clone().requestFactory(requestFactory).build();
        return uri -> restClient.get().uri(uri).accept(MediaType.parseMediaType("text/csv")).retrieve()
                .body(String.class);
    }

    /**
     * @param dataset manifest entry of the dataset
     * @return the data rows of the sample (header excluded), empty when the dataset has no sample or it is unreadable
     */
    public List<List<String>> rows(Optional<DatasetManifest> dataset) {
        if (dataset.isEmpty() || !StringUtils.hasText(dataset.get().sample())
                || !StringUtils.hasText(openDataConfig.getPublicBaseUrl())) {
            return List.of();
        }
        DatasetManifest entry = dataset.get();
        String key = entry.sample() + "@" + entry.generatedAt();
        List<List<String>> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Long retryAt = failures.get(key);
        if (retryAt != null && clock.getAsLong() < retryAt) {
            return List.of();
        }
        try {
            List<List<String>> rows = parse(reader.apply(sampleUri(entry.sample())));
            cache.keySet().removeIf(old -> old.startsWith(entry.sample() + "@"));
            cache.put(key, rows);
            failures.remove(key);
            return rows;
        } catch (IOException | CsvValidationException | RuntimeException e) {
            LOGGER.warn("Open data sample {} unreadable, the extract is not rendered: {}", entry.sample(),
                    e.getMessage());
            failures.keySet().removeIf(old -> old.startsWith(entry.sample() + "@"));
            failures.put(key, clock.getAsLong() + retryDelay.toMillis());
            return List.of();
        }
    }

    private URI sampleUri(String sample) {
        String base = openDataConfig.getPublicBaseUrl().trim();
        return URI.create((base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + sample);
    }

    static List<List<String>> parse(String csv) throws IOException, CsvValidationException {
        List<List<String>> rows = new ArrayList<>();
        if (csv == null) {
            return rows;
        }
        try (CSVReader reader = new CSVReader(new StringReader(csv))) {
            reader.readNext(); // header, already known from the manifest columns
            String[] line;
            while ((line = reader.readNext()) != null && rows.size() < 20) {
                rows.add(List.of(line));
            }
        }
        return List.copyOf(rows);
    }
}

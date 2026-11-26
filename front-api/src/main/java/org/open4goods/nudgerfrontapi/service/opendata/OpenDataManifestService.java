package org.open4goods.nudgerfrontapi.service.opendata;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.open4goods.nudgerfrontapi.config.properties.OpenDataManifestProperties;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the published {@code manifest.json} of the open data files (GOU-2290), the source of {@code /opendata/*}.
 * <p>
 * The manifest is the one written by the api ({@code OpenDataService}, {@code OpenDataPublisher}) and served by
 * {@code {open-data-config.public-base-url}/manifest.json} ({@code data.nudger.fr} in prod), parsed into the same
 * {@link OpenDataManifest} records; unknown fields are ignored (an added field is a compatible change).
 * </p>
 * <ul>
 * <li>Empty {@code public-base-url}: {@link OpenDataManifestSnapshot#NOT_CONFIGURED}, never an HTTP call.</li>
 * <li>One read per {@code front.opendata.manifest-cache-ttl} (5 min), successful or not, with short timeouts; a single
 * caller reads, the others get the previous snapshot meanwhile.</li>
 * <li>A failed read (HTTP error, network, invalid JSON) never throws: the snapshot is {@code UNAVAILABLE} and keeps the
 * last manifest read, if any. A WARN is logged when the reason changes, not on every read.</li>
 * <li>Gauge {@code opendata.dataset.age{dataset}} (Prometheus {@code opendata_dataset_age_seconds}): now minus the
 * {@code generatedAt} of each dataset of the manifest, NaN when unknown.</li>
 * </ul>
 */
@Service
public class OpenDataManifestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataManifestService.class);

    /** Identifier of the GTIN dataset in the manifest. */
    public static final String GTIN = "gtin";

    /** Identifier of the ISBN dataset in the manifest. */
    public static final String ISBN = "isbn";

    /** Age of each published dataset, in seconds ({@code opendata_dataset_age_seconds}). */
    public static final String AGE_METRIC = "opendata.dataset.age";

    private static final String DATASET_TAG = "dataset";
    private static final int MAX_ERROR_LENGTH = 200;

    /**
     * Reads the body of the manifest; throws on an HTTP error or a network failure.
     */
    @FunctionalInterface
    interface ManifestReader {

        /**
         * @param uri URL of the manifest
         * @return its body
         */
        String read(URI uri);
    }

    private final OpenDataConfig openDataConfig;
    private final OpenDataManifestProperties properties;
    private final ManifestReader reader;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private final ReentrantLock readLock = new ReentrantLock();
    private final Set<String> ageGauges = ConcurrentHashMap.newKeySet();

    private volatile OpenDataManifestSnapshot snapshot;
    private volatile Instant nextReadAt = Instant.MIN;
    private volatile String lastLoggedError;

    /**
     * @param openDataConfig    {@code open-data-config}: public base URL and manifest name
     * @param properties        cache, timeouts and maximum age ({@code front.opendata})
     * @param restClientBuilder shared builder, cloned with the manifest timeouts (never mutated)
     * @param meterRegistry     registry of the age gauges
     */
    @Autowired
    public OpenDataManifestService(OpenDataConfig openDataConfig,
                                   OpenDataManifestProperties properties,
                                   RestClient.Builder restClientBuilder,
                                   ObjectProvider<MeterRegistry> meterRegistry) {
        this(openDataConfig, properties, restReader(restClientBuilder, properties),
                meterRegistry.getIfAvailable(SimpleMeterRegistry::new), Clock.systemUTC());
    }

    OpenDataManifestService(OpenDataConfig openDataConfig,
                            OpenDataManifestProperties properties,
                            ManifestReader reader,
                            MeterRegistry meterRegistry,
                            Clock clock) {
        this.openDataConfig = openDataConfig;
        this.properties = properties;
        this.reader = reader;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        registerAgeGauge(GTIN);
        registerAgeGauge(ISBN);
    }

    /**
     * Builds the reader used in production: JDK HTTP client with the configured connection and read timeouts.
     */
    static ManifestReader restReader(RestClient.Builder restClientBuilder, OpenDataManifestProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        RestClient restClient = restClientBuilder.clone().requestFactory(requestFactory).build();
        return uri -> restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
    }

    /**
     * @return true when {@code open-data-config.public-base-url} is set, the only case where the manifest is read
     */
    public boolean isConfigured() {
        return StringUtils.hasText(openDataConfig.getPublicBaseUrl());
    }

    /**
     * @return URL of the manifest, {@code null} when no public base URL is configured
     */
    public String manifestUrl() {
        if (!isConfigured()) {
            return null;
        }
        String base = openDataConfig.getPublicBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + openDataConfig.getManifestFilename();
    }

    /**
     * Current manifest, read again when the previous read is older than the cache TTL. Never throws.
     *
     * @return the snapshot of the last read, {@link OpenDataManifestSnapshot#NOT_CONFIGURED} without a base URL
     */
    public OpenDataManifestSnapshot snapshot() {
        if (!isConfigured()) {
            return OpenDataManifestSnapshot.NOT_CONFIGURED;
        }
        OpenDataManifestSnapshot current = snapshot;
        if (current != null && clock.instant().isBefore(nextReadAt)) {
            return current;
        }
        if (current == null) {
            // First read: every caller waits for it (bounded by the timeouts).
            readLock.lock();
        } else if (!readLock.tryLock()) {
            // Another caller reads: serve the previous snapshot meanwhile.
            return current;
        }
        try {
            current = snapshot;
            if (current != null && clock.instant().isBefore(nextReadAt)) {
                return current;
            }
            OpenDataManifestSnapshot next = read(current);
            snapshot = next;
            nextReadAt = clock.instant().plus(properties.getManifestCacheTtl());
            return next;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @param dataset entry of the manifest
     * @return now minus its {@code generatedAt}, empty when the date is absent or invalid
     */
    public Optional<Duration> age(DatasetManifest dataset) {
        return OpenDataManifestSnapshot.generatedAt(dataset).map(at -> Duration.between(at, clock.instant()));
    }

    /**
     * @return age above which a dataset is stale ({@code front.opendata.max-age}, 8 days)
     */
    public Duration maxAge() {
        return properties.getMaxAge();
    }

    /**
     * Reads the manifest once; a failure keeps the previous manifest.
     */
    private OpenDataManifestSnapshot read(OpenDataManifestSnapshot previous) {
        String url = manifestUrl();
        try {
            OpenDataManifest manifest = parse(reader.read(URI.create(url)));
            manifest.datasets().stream()
                    .filter(entry -> entry != null && StringUtils.hasText(entry.id()))
                    .forEach(entry -> registerAgeGauge(entry.id()));
            if (lastLoggedError != null) {
                LOGGER.info("Open data manifest {} is readable again", url);
                lastLoggedError = null;
            }
            return new OpenDataManifestSnapshot(OpenDataManifestSnapshot.State.AVAILABLE, manifest, clock.instant(),
                    null);
        } catch (RuntimeException e) {
            String error = describe(e);
            OpenDataManifest kept = previous == null ? null : previous.manifest();
            if (!error.equals(lastLoggedError)) {
                LOGGER.warn("Open data manifest {} unavailable ({}): {}", url, error,
                        kept == null ? "its values are served as null" : "the last manifest read is kept");
                lastLoggedError = error;
            } else {
                LOGGER.debug("Open data manifest {} still unavailable ({})", url, error);
            }
            return new OpenDataManifestSnapshot(OpenDataManifestSnapshot.State.UNAVAILABLE, kept,
                    previous == null ? null : previous.readAt(), error);
        }
    }

    /**
     * @throws IllegalArgumentException or a Jackson exception when the body is not a manifest
     */
    private OpenDataManifest parse(String body) {
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("empty body");
        }
        OpenDataManifest manifest = mapper.readValue(body, OpenDataManifest.class);
        if (manifest == null || manifest.datasets() == null) {
            throw new IllegalArgumentException("no datasets in the manifest");
        }
        return manifest;
    }

    /**
     * @return a short, single-line reason for the logs and the health details
     */
    private static String describe(RuntimeException e) {
        if (e instanceof RestClientResponseException response) {
            return "HTTP " + response.getStatusCode().value();
        }
        String text = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) + "..." : text;
    }

    /**
     * Registers the age gauge of a dataset once (GTIN and ISBN at startup, the others when first seen).
     */
    private void registerAgeGauge(String dataset) {
        if (ageGauges.add(dataset)) {
            Gauge.builder(AGE_METRIC, this, service -> service.ageSeconds(dataset))
                    .description("Age of the published open data file (now minus its manifest generatedAt), NaN when "
                            + "unknown")
                    .baseUnit("seconds")
                    .tag(DATASET_TAG, dataset)
                    .register(meterRegistry);
        }
    }

    /**
     * @return age of a dataset in seconds, NaN when not configured, not read or without a valid date
     */
    double ageSeconds(String dataset) {
        return snapshot().dataset(dataset)
                .flatMap(this::age)
                .map(age -> (double) age.toSeconds())
                .orElse(Double.NaN);
    }
}

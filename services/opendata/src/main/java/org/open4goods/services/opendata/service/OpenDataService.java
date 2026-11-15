package org.open4goods.services.opendata.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Product;
import org.open4goods.services.opendata.DatasetDefinition;
import org.open4goods.services.opendata.DatasetWriter;
import org.open4goods.services.opendata.DatasetWriterManager;
import org.open4goods.services.opendata.OpenDataGenerationException;
import org.open4goods.services.opendata.ThrottlingInputStream;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.ColumnManifest;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus.State;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.open4goods.services.opendata.model.OpenDataPublicationStatus;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.util.concurrent.RateLimiter;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Service responsible for generating and managing the CSV OpenData files.
 * Handles the creation of GTIN and ISBN datasets, their compression into ZIP files, their manifests, and the management
 * of download limits and rates. Generation runs in the api only (GOU-2248, {@code generation-enabled}); the schedule
 * lives in the api, this bean has none. After a successful pass, {@link OpenDataPublisher} publishes the files on R2
 * when {@code open-data-config.publication.enabled} is set (GOU-2289).
 */
@Service
public class OpenDataService implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataService.class);

    private static final String ISBN_DATASET_FILENAME = "open4goods-isbn-dataset.csv";
    private static final String GTIN_DATASET_FILENAME = "open4goods-gtin-dataset.csv";

    public static final String[] GTIN_HEADER = {
            "gtin",
            "brand",
            "model",
            "name",
            "last_updated",
            "gs1_country",
            "gtinType",
            "offers_count",
            "min_price",
            "currency",
            "categories",
            "url",
            "sources"
    };

    public static final String[] ISBN_HEADER = {
            "isbn",
            "title",
            "last_updated",
            "offers_count",
            "min_price",
            "currency",
            "url",
            "editeur",
            "format",
            "nb_page",
            "souscategorie",
            "souscategorie2",
            "sources"
    };

    private static final List<String> ISBN_ATTRIBUTE_KEYS = List.of(
            "EDITEUR",
            "FORMAT",
            "NB DE PAGES",
            "SOUSCATEGORIE",
            "SOUSCATEGORIE2"
    );

    /**
     * Only {@code _source} fields read by the export (GOU-2248): about 0.7 KB per product instead of about 21 KB for
     * a complete product, whose {@code attributes} and offers made a page of 5000 exceed the 100 MB response buffer of
     * the Elasticsearch client (GOU-2249). No other attribute key is read, in particular no Decitre key (GOU-2262).
     */
    static final String[] EXPORT_SOURCE_FIELDS = exportSourceFields();

    private static final Set<BarcodeType> GTIN_BARCODE_TYPES =
            Set.of(BarcodeType.GTIN_8, BarcodeType.GTIN_12, BarcodeType.GTIN_13, BarcodeType.GTIN_14);
    private static final Set<BarcodeType> ISBN_BARCODE_TYPES = Set.of(BarcodeType.ISBN_13);

    /** Longest pause allowed between two pages: well below the one-minute scroll keep-alive of Spring Data. */
    private static final Duration MAX_PAGE_PAUSE = Duration.ofSeconds(30);

    private static final String DATASET_TAG = "dataset";
    private static final String OUTCOME_TAG = "outcome";

    private final ProductRepository aggregatedDataRepository;
    private final OpenDataConfig openDataConfig;
    private final boolean generationEnabled;
    private final OpenDataManifestStore manifestStore = new OpenDataManifestStore();
    private final MeterRegistry meterRegistry;
    private final OpenDataPublisher publisher;

    private final AtomicBoolean exportRunning = new AtomicBoolean(false);
    private final AtomicInteger concurrentDownloadsCounter = new AtomicInteger(0);
    private volatile OpenDataGenerationStatus status = OpenDataGenerationStatus.idle();
    private final Map<String, AtomicLong> exportedRows = new ConcurrentHashMap<>();

    @Autowired
    public OpenDataService(ProductRepository aggregatedDataRepository,
                           OpenDataConfig openDataConfig,
                           ObjectProvider<MeterRegistry> meterRegistry,
                           OpenDataPublisher publisher) {
        this(aggregatedDataRepository, openDataConfig, meterRegistry.getIfAvailable(SimpleMeterRegistry::new), publisher);
    }

    OpenDataService(ProductRepository aggregatedDataRepository,
                    OpenDataConfig openDataConfig,
                    MeterRegistry meterRegistry) {
        this(aggregatedDataRepository, openDataConfig, meterRegistry,
                new OpenDataPublisher(openDataConfig, meterRegistry, S3OpenDataStorage::new));
    }

    OpenDataService(ProductRepository aggregatedDataRepository,
                    OpenDataConfig openDataConfig,
                    MeterRegistry meterRegistry,
                    OpenDataPublisher publisher) {
        Assert.notNull(aggregatedDataRepository, "aggregatedDataRepository must not be null");
        Assert.notNull(openDataConfig, "openDataConfig must not be null");
        Assert.notNull(openDataConfig.getGenerationEnabled(), "openDataConfig.generationEnabled must be configured");
        Assert.notNull(meterRegistry, "meterRegistry must not be null");
        Assert.notNull(publisher, "publisher must not be null");

        this.aggregatedDataRepository = aggregatedDataRepository;
        this.openDataConfig = openDataConfig;
        this.generationEnabled = openDataConfig.isGenerationEnabled();
        this.meterRegistry = meterRegistry;
        this.publisher = publisher;
        registerGauges();

        LOGGER.info("OpenDataService generation is {}", generationEnabled ? "enabled" : "disabled");
    }

    private static String[] exportSourceFields() {
        List<String> fields = new ArrayList<>(List.of(
                "id",
                "lastChange",
                "offersCount",
                "offerNames",
                "datasourceCategories",
                "sources",
                "gtinInfos.upcType",
                "gtinInfos.country",
                "price.minPrice.price",
                "price.minPrice.currency",
                "attributes.referentielAttributes"));
        for (String key : ISBN_ATTRIBUTE_KEYS) {
            fields.add("attributes.indexed." + key);
            fields.add("attributes.all." + key);
        }
        return fields.toArray(new String[0]);
    }

    private void registerGauges() {
        Gauge.builder("opendata.generation.running", exportRunning, running -> running.get() ? 1 : 0)
                .description("1 while an open data generation pass runs")
                .register(meterRegistry);
        Gauge.builder("opendata.generation.failed", this, service -> service.status.state() == State.FAILED ? 1 : 0)
                .description("1 when the last open data generation pass of this process failed")
                .register(meterRegistry);
        Gauge.builder("opendata.generation.last.success.timestamp", this, OpenDataService::lastSuccessEpochSeconds)
                .description("End of the last successful open data generation pass of this process (NaN if none)")
                .baseUnit("seconds")
                .register(meterRegistry);
        for (String dataset : List.of("gtin", "isbn")) {
            AtomicLong rows = exportedRows.computeIfAbsent(dataset, key -> new AtomicLong());
            Gauge.builder("opendata.dataset.rows", rows, AtomicLong::get)
                    .description("Rows of the last open data file published by this process")
                    .tag(DATASET_TAG, dataset)
                    .register(meterRegistry);
        }
    }

    private double lastSuccessEpochSeconds() {
        Instant lastSuccess = status.lastSuccessAt();
        return lastSuccess == null ? Double.NaN : lastSuccess.getEpochSecond();
    }

    /**
     * DOWN when the last generation pass or the last R2 publication of this process failed (the previous files stay
     * published), UNKNOWN when a published zip is missing and nothing failed, UP otherwise.
     */
    @Override
    public Health health() {
        OpenDataGenerationStatus current = status;
        if (current.state() == State.FAILED) {
            return withStatusDetails(Health.down(), current)
                    .withDetail("error", StringUtils.defaultString(current.error()))
                    .build();
        }
        OpenDataPublicationStatus publication = publisher.status();
        if (publication.state() == State.FAILED) {
            return withStatusDetails(Health.down(), current)
                    .withDetail("publicationError", StringUtils.defaultString(publication.error()))
                    .build();
        }
        if (!openDataConfig.isbnZipFile().exists() || !openDataConfig.gtinZipFile().exists()) {
            return withStatusDetails(Health.unknown(), current)
                    .withDetail("files", "Le fichier ZIP GTIN ou ISBN est manquant.")
                    .build();
        }
        return withStatusDetails(Health.up(), current).build();
    }

    private Health.Builder withStatusDetails(Health.Builder builder, OpenDataGenerationStatus current) {
        builder.withDetail("state", current.state().name());
        if (current.startedAt() != null) {
            builder.withDetail("startedAt", current.startedAt().toString());
        }
        if (current.finishedAt() != null) {
            builder.withDetail("finishedAt", current.finishedAt().toString());
        }
        if (current.lastSuccessAt() != null) {
            builder.withDetail("lastSuccessAt", current.lastSuccessAt().toString());
        }
        OpenDataPublicationStatus publication = publisher.status();
        builder.withDetail("publication", publisher.isEnabled() ? publication.state().name() : "DISABLED");
        if (publication.lastSuccessAt() != null) {
            builder.withDetail("publicationLastSuccessAt", publication.lastSuccessAt().toString());
        }
        return builder;
    }

    /**
     * @return the generation state of this process
     */
    public OpenDataGenerationStatus status() {
        return status;
    }

    /**
     * Provides a limited bandwidth stream for downloading the opendata file.
     * Enforces a limit on the number of concurrent downloads.
     */
    public InputStream limitedRateStream(String file) throws TechnicalException, FileNotFoundException {

        // The limiter enforces a maximum bandwidth per download so that a single
        // client cannot saturate the outbound network interface.
        RateLimiter rateLimiter = RateLimiter.create(openDataConfig.getDownloadSpeedKb() * FileUtils.ONE_KB);

        if (concurrentDownloadsCounter.get() >= openDataConfig.getConcurrentDownloads()) {
            throw new TechnicalException("Too many requests ");
        } else {
            concurrentDownloadsCounter.incrementAndGet();
        }

        try {
            LOGGER.info("Starting opendata dataset download");
            return new ThrottlingInputStream(new BufferedInputStream(new FileInputStream(file)),
                    rateLimiter) {
                @Override
                public void close() throws IOException {
                    super.close();
                    concurrentDownloadsCounter.decrementAndGet();
                    LOGGER.info("Ending opendata dataset download");
                }
            };
        } catch (IOException e) {
            concurrentDownloadsCounter.decrementAndGet();
            throw e;
        }
    }

    /**
     * Starts a generation pass on a dedicated thread (a pass lasts hours: it must not hold a scheduler thread).
     *
     * @return false when generation is disabled by configuration or a pass already runs
     */
    public boolean launchGeneration() {
        if (!acquire()) {
            return false;
        }
        Thread.ofPlatform().name("opendata-generation").daemon(true).start(() -> {
            try {
                runPass();
            } finally {
                exportRunning.set(false);
            }
        });
        return true;
    }

    /**
     * Runs a generation pass in the calling thread.
     *
     * @return true when new files and manifests were published, false when the pass was skipped or failed
     */
    public boolean generateOpendata() {
        if (!acquire()) {
            return false;
        }
        try {
            return runPass();
        } finally {
            exportRunning.set(false);
        }
    }

    private boolean acquire() {
        if (!generationEnabled) {
            LOGGER.warn("Open data generation skipped: open-data-config.generation-enabled is false");
            return false;
        }
        if (exportRunning.getAndSet(true)) {
            LOGGER.warn("Open data generation skipped: a pass is already running");
            return false;
        }
        return true;
    }

    /**
     * Runs one pass, then the R2 publication when enabled. Any failure of the pass is logged at ERROR, counted, and
     * exposed by {@link #health()} and {@link #status()}; the published files and manifests are then unchanged and
     * nothing is sent. A failure of the publication has its own status and metrics ({@link OpenDataPublisher}).
     */
    private boolean runPass() {
        Instant startedAt = Instant.now();
        OpenDataGenerationStatus previous = status;
        status = new OpenDataGenerationStatus(State.RUNNING, startedAt, null, previous.lastSuccessAt(), null, previous.rows());
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        List<DatasetManifest> published;
        try {
            published = generate();
            Map<String, Long> rows = new LinkedHashMap<>();
            published.forEach(manifest -> rows.put(manifest.id(), manifest.rows()));
            Instant finishedAt = Instant.now();
            rows.forEach((dataset, count) -> exportedRows.computeIfAbsent(dataset, key -> new AtomicLong()).set(count));
            status = new OpenDataGenerationStatus(State.SUCCEEDED, startedAt, finishedAt, finishedAt, null, rows);
            outcome = "success";
            LOGGER.info("Open data generation succeeded in {}: {}", Duration.between(startedAt, finishedAt), rows);
        } catch (Exception e) {
            LOGGER.error("Open data generation failed, the published files and manifests are unchanged", e);
            status = new OpenDataGenerationStatus(State.FAILED, startedAt, Instant.now(), previous.lastSuccessAt(),
                    e.getClass().getSimpleName() + ": " + e.getMessage(), previous.rows());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            deleteTmpDirectory();
            sample.stop(meterRegistry.timer("opendata.generation.duration", OUTCOME_TAG, outcome));
            meterRegistry.counter("opendata.generation.runs", OUTCOME_TAG, outcome).increment();
        }
        if (publisher.isEnabled()) {
            publisher.publish(publicationDatasets(published), openDataConfig.manifestFile().toPath());
        }
        return true;
    }

    /**
     * @return the datasets just published locally, in the order of {@link #datasetDefinitions()}
     */
    private List<OpenDataPublisher.Dataset> publicationDatasets(List<DatasetManifest> manifests) {
        List<DatasetDefinition> definitions = datasetDefinitions();
        List<OpenDataPublisher.Dataset> datasets = new ArrayList<>();
        for (int i = 0; i < definitions.size(); i++) {
            datasets.add(new OpenDataPublisher.Dataset(manifests.get(i), definitions.get(i).finalZipFile().toPath(),
                    definitions.get(i).manifestFile().toPath()));
        }
        return datasets;
    }

    /**
     * Writes both datasets in the temporary folder, checks them, then publishes them: zips first, per-dataset
     * manifests next, {@code manifest.json} last.
     *
     * @return the published manifests, in the order of {@link #datasetDefinitions()}
     */
    List<DatasetManifest> generate() throws IOException, OpenDataGenerationException, InterruptedException {
        List<DatasetDefinition> datasets = datasetDefinitions();
        prepareTmpDirectory();
        checkFreeSpace(datasets);

        Map<String, Long> indexCounts = new LinkedHashMap<>();
        for (DatasetDefinition dataset : datasets) {
            indexCounts.put(dataset.id(), aggregatedDataRepository.countItemsByBarcodeType(
                    dataset.barcodeTypes().toArray(new BarcodeType[0])));
        }

        List<DatasetWriter> writers = writeDatasets(datasets);

        List<DatasetManifest> manifests = new ArrayList<>();
        for (DatasetWriter writer : writers) {
            manifests.add(buildManifest(writer, indexCounts.getOrDefault(writer.definition().id(), 0L)));
        }
        for (int i = 0; i < manifests.size(); i++) {
            checkRows(manifests.get(i), datasets.get(i));
        }

        publish(datasets, manifests);
        return manifests;
    }

    /**
     * @return the GTIN then ISBN datasets
     */
    List<DatasetDefinition> datasetDefinitions() {
        DatasetDefinition gtinDataset = new DatasetDefinition(
                "gtin",
                GTIN_DATASET_FILENAME,
                openDataConfig.tmpGtinZipFile(),
                openDataConfig.gtinZipFile(),
                openDataConfig.gtinManifestFile(),
                openDataConfig.getGtinPublicFilename(),
                GTIN_HEADER,
                GTIN_BARCODE_TYPES,
                this::toGtinEntry
        );

        DatasetDefinition isbnDataset = new DatasetDefinition(
                "isbn",
                ISBN_DATASET_FILENAME,
                openDataConfig.tmpIsbnZipFile(),
                openDataConfig.isbnZipFile(),
                openDataConfig.isbnManifestFile(),
                openDataConfig.getIsbnPublicFilename(),
                ISBN_HEADER,
                ISBN_BARCODE_TYPES,
                this::toIsbnEntry
        );

        return List.of(gtinDataset, isbnDataset);
    }

    private void prepareTmpDirectory() throws IOException {
        File tmpDirectory = openDataConfig.tmpDirectory().toFile();
        if (tmpDirectory.exists()) {
            LOGGER.warn("Removing the open data temporary folder left by a previous pass: {}", tmpDirectory);
            FileUtils.deleteDirectory(tmpDirectory);
        }
        Files.createDirectories(openDataConfig.tmpDirectory());
    }

    private void deleteTmpDirectory() {
        File tmpDirectory = openDataConfig.tmpDirectory().toFile();
        if (tmpDirectory.exists() && !FileUtils.deleteQuietly(tmpDirectory)) {
            LOGGER.error("Unable to delete the open data temporary folder {}", tmpDirectory);
        }
    }

    /**
     * Fails before any read when the free space is below {@code min-free-space-ratio} times the published files.
     */
    private void checkFreeSpace(List<DatasetDefinition> datasets) throws IOException, OpenDataGenerationException {
        long published = datasets.stream()
                .map(DatasetDefinition::finalZipFile)
                .filter(File::isFile)
                .mapToLong(File::length)
                .sum();
        long required = (long) Math.ceil(published * openDataConfig.getMinFreeSpaceRatio());
        long usable = usableSpace(openDataConfig.tmpDirectory());
        if (usable < required) {
            throw new OpenDataGenerationException(String.format(Locale.ROOT,
                    "Not enough free disk space in %s: %d bytes usable, %d required (%.2f x the %d bytes published)",
                    openDataConfig.tmpDirectory(), usable, required, openDataConfig.getMinFreeSpaceRatio(), published));
        }
    }

    /**
     * @return usable bytes on the file store of {@code directory}
     */
    long usableSpace(Path directory) throws IOException {
        return Files.getFileStore(directory).getUsableSpace();
    }

    /**
     * Streams the projected products page by page into the dataset writers, pausing between pages.
     *
     * @return the closed writers, in the order of {@code datasetDefinitions}
     */
    List<DatasetWriter> writeDatasets(List<DatasetDefinition> datasetDefinitions) throws IOException, InterruptedException {
        int pageSize = Math.max(1, openDataConfig.getExportPageSize());
        Duration pause = openDataConfig.getPagePause() == null ? Duration.ZERO : openDataConfig.getPagePause();
        if (pause.compareTo(MAX_PAGE_PAUSE) > 0) {
            throw new IllegalArgumentException("open-data-config.page-pause must not exceed " + MAX_PAGE_PAUSE);
        }

        try (DatasetWriterManager manager = new DatasetWriterManager(datasetDefinitions)) {
            EnumMap<BarcodeType, DatasetWriter> writerByType = mapWritersByType(manager.writers());
            long read = 0;
            try (Stream<Product> stream = aggregatedDataRepository.exportAll(EXPORT_SOURCE_FIELDS, pageSize)) {
                Iterator<Product> products = stream.iterator();
                while (products.hasNext()) {
                    Product product = products.next();
                    read++;
                    BarcodeType barcodeType = resolveBarcodeType(product);
                    DatasetWriter writer = barcodeType == null ? null : writerByType.get(barcodeType);
                    if (writer != null) {
                        writer.write(product);
                    }
                    if (read % pageSize == 0) {
                        if (!pause.isZero()) {
                            Thread.sleep(pause);
                        }
                        if (read % (pageSize * 500L) == 0) {
                            LOGGER.info("Open data generation: {} products read", read);
                        }
                    }
                }
            }
            LOGGER.info("Open data generation: {} products read", read);
            manager.close();
            manager.writers().forEach(writer ->
                    LOGGER.info("{} rows exported in {}.", writer.count(), writer.definition().filename()));
            return manager.writers();
        }
    }

    private DatasetManifest buildManifest(DatasetWriter writer, long indexCount) throws IOException {
        DatasetDefinition definition = writer.definition();
        long rows = writer.count();
        List<ColumnManifest> columns = new ArrayList<>();
        for (int i = 0; i < definition.header().length; i++) {
            double fillRate = rows == 0 ? 0 : Math.round(writer.filledCells(i) * 10_000d / rows) / 10_000d;
            columns.add(new ColumnManifest(definition.header()[i], fillRate));
        }
        return new DatasetManifest(
                OpenDataManifest.SCHEMA_VERSION,
                definition.id(),
                definition.publicFilename(),
                publicUrl(definition.publicFilename()),
                DatasetManifest.FORMAT,
                OpenDataManifest.LICENSE,
                Files.size(definition.zipFile().toPath()),
                writer.sha256(),
                rows,
                indexCount,
                now(),
                columns);
    }

    private String publicUrl(String publicFilename) {
        String base = openDataConfig.getPublicBaseUrl();
        if (StringUtils.isBlank(base)) {
            return null;
        }
        return StringUtils.removeEnd(base.trim(), "/") + "/" + publicFilename;
    }

    /**
     * Row guard: refuses a dataset that lost more than {@code 1 - min-rows-ratio} of the rows of the published manifest,
     * or of the products counted in the index at the start of the pass (an export that stopped early).
     */
    private void checkRows(DatasetManifest manifest, DatasetDefinition definition) throws OpenDataGenerationException {
        double ratio = openDataConfig.getMinRowsRatio();
        Optional<DatasetManifest> published = manifestStore.readDataset(definition.manifestFile());
        if (published.isPresent() && published.get().rows() > 0 && manifest.rows() < published.get().rows() * ratio) {
            throw new OpenDataGenerationException(String.format(Locale.ROOT,
                    "Dataset %s refused: %d rows, the published manifest has %d (minimum ratio %.2f)",
                    manifest.id(), manifest.rows(), published.get().rows(), ratio));
        }
        if (manifest.indexCount() > 0 && manifest.rows() < manifest.indexCount() * ratio) {
            throw new OpenDataGenerationException(String.format(Locale.ROOT,
                    "Dataset %s refused: %d rows, the index counted %d products (minimum ratio %.2f)",
                    manifest.id(), manifest.rows(), manifest.indexCount(), ratio));
        }
    }

    /**
     * Writes the manifests in the temporary folder, then renames atomically the zips, the per-dataset manifests and
     * {@code manifest.json}, in that order.
     */
    private void publish(List<DatasetDefinition> datasets, List<DatasetManifest> manifests) throws IOException {
        Path tmpDirectory = openDataConfig.tmpDirectory();
        for (int i = 0; i < datasets.size(); i++) {
            manifestStore.write(manifests.get(i), tmpDirectory.resolve(datasets.get(i).manifestFile().getName()).toFile());
        }
        File tmpManifest = tmpDirectory.resolve(openDataConfig.manifestFile().getName()).toFile();
        manifestStore.write(new OpenDataManifest(OpenDataManifest.SCHEMA_VERSION, now(), OpenDataManifest.LICENSE,
                manifests), tmpManifest);

        for (DatasetDefinition dataset : datasets) {
            OpenDataManifestStore.moveAtomically(dataset.zipFile().toPath(), dataset.finalZipFile().toPath());
        }
        for (DatasetDefinition dataset : datasets) {
            OpenDataManifestStore.moveAtomically(tmpDirectory.resolve(dataset.manifestFile().getName()),
                    dataset.manifestFile().toPath());
        }
        OpenDataManifestStore.moveAtomically(tmpManifest.toPath(), openDataConfig.manifestFile().toPath());
        manifests.forEach(manifest -> LOGGER.info("Open data {} published: {} rows, {} bytes, sha256 {}",
                manifest.id(), manifest.rows(), manifest.bytes(), manifest.sha256()));
    }

    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private EnumMap<BarcodeType, DatasetWriter> mapWritersByType(List<DatasetWriter> writers) {
        EnumMap<BarcodeType, DatasetWriter> writerByType = new EnumMap<>(BarcodeType.class);
        for (DatasetWriter writer : writers) {
            for (BarcodeType barcodeType : writer.definition().barcodeTypes()) {
                DatasetWriter existing = writerByType.put(barcodeType, writer);
                if (existing != null && existing != writer) {
                    throw new IllegalStateException("Barcode type " + barcodeType + " is mapped to multiple datasets");
                }
            }
        }
        return writerByType;
    }

    private BarcodeType resolveBarcodeType(Product product) {
        if (product.getGtinInfos() == null) {
            return null;
        }
        return product.getGtinInfos().getUpcType();
    }

    private Map<String, String> extractAttributes(Product data, Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        ProductAttributes attributes = data.getAttributes();
        if (attributes == null) {
            return Collections.emptyMap();
        }

        Map<String, IndexedAttribute> indexed = attributes.getIndexed();
        Map<String, ProductAttribute> all = attributes.getAll();
        if ((indexed == null || indexed.isEmpty()) && (all == null || all.isEmpty())) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        for (String key : keys) {
            String value = null;
            if (indexed != null) {
                IndexedAttribute indexedAttribute = indexed.get(key);
                if (indexedAttribute != null) {
                    value = indexedAttribute.getValue();
                }
            }
            if (value == null && all != null) {
                ProductAttribute productAttribute = all.get(key);
                if (productAttribute != null) {
                    value = productAttribute.getValue();
                }
            }
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }

    /**
     * Picks the shortest offer name that is not corrupted by an encoding loss ({@code ¿} or {@code �}). Returns an
     * empty string when every name is corrupted: a missing name is better than a wrong one.
     */
    static String exportName(Product data) {
        Set<String> names = data.getOfferNames();
        if (names == null) {
            return "";
        }
        return names.stream()
                .filter(n -> n != null && n.indexOf('\u00BF') < 0 && n.indexOf('\uFFFD') < 0)
                .min(java.util.Comparator.comparingInt(String::length))
                .orElse("");
    }

    /**
     * Converts a Product object into a CSV row for GTIN.
     */
    private String[] toGtinEntry(Product data) {
        String[] line = new String[GTIN_HEADER.length];
        Arrays.fill(line, "");

        line[0] = StringUtils.defaultString(data.gtin());
        line[1] = StringUtils.defaultString(data.brand());
        line[2] = StringUtils.defaultString(data.model());
        line[3] = exportName(data);

        line[4] = String.valueOf(data.getLastChange());

        if (data.getGtinInfos() != null) {
            line[5] = StringUtils.defaultString(data.getGtinInfos().getCountry());
            if (data.getGtinInfos().getUpcType() != null) {
                line[6] = data.getGtinInfos().getUpcType().toString();
            }
        }
        if (data.getOffersCount() != null) {
            line[7] = String.valueOf(data.getOffersCount());
        }

        AggregatedPrice bestPrice = data.bestPrice();
        if (bestPrice != null) {
            if (bestPrice.getPrice() != null) {
                line[8] = String.valueOf(bestPrice.getPrice());
            }
            if (bestPrice.getCurrency() != null) {
                line[9] = bestPrice.getCurrency().toString();
            }
        }

        Collection<String> categories = data.getDatasourceCategories();
        if (categories != null && !categories.isEmpty()) {
            line[10] = StringUtils.join(categories, " ; ");
        }

        try {
            // TODO : Point to international website + internationalized URL
            String url = "https://nudger.fr/" + StringUtils.defaultString(data.gtin());
            line[11] = url;
        } catch (Exception e) {
            LOGGER.error("Error while extracting URL for GTIN {}", data.getId(), e);
        }
        line[12] = exportSources(data);

        return line;
    }

    /** The contributing sources of a product, identifiers separated by {@code |} (GOU-2301). */
    private static String exportSources(Product data) {
        return data.getSources() == null ? "" : StringUtils.join(data.getSources(), "|");
    }

    /**
     * Converts a Product object into a CSV row for ISBN.
     */
    private String[] toIsbnEntry(Product data) {
        String[] line = new String[ISBN_HEADER.length];
        Arrays.fill(line, "");

        line[0] = StringUtils.defaultString(data.gtin());
        line[1] = exportName(data);
        line[2] = String.valueOf(data.getLastChange());
        if (data.getOffersCount() != null) {
            line[3] = String.valueOf(data.getOffersCount());
        }

        AggregatedPrice bestPrice = data.bestPrice();
        if (bestPrice != null) {
            if (bestPrice.getPrice() != null) {
                line[4] = String.valueOf(bestPrice.getPrice());
            }
            if (bestPrice.getCurrency() != null) {
                line[5] = bestPrice.getCurrency().toString();
            }
        }
        try {
            // TODO : Point to international website + internationalized URL
            String url = "https://nudger.fr/" + StringUtils.defaultString(data.gtin());
            line[6] = url;
        } catch (Exception e) {
            LOGGER.error("Error while extracting URL for ISBN {}", data.getId(), e);
        }

        Map<String, String> isbnAttributes = extractAttributes(data, ISBN_ATTRIBUTE_KEYS);
        line[7] = StringUtils.defaultString(isbnAttributes.get("EDITEUR"));
        line[8] = StringUtils.defaultString(isbnAttributes.get("FORMAT"));
        line[9] = StringUtils.defaultString(isbnAttributes.get("NB DE PAGES"));
        line[10] = StringUtils.defaultString(isbnAttributes.get("SOUSCATEGORIE"));
        line[11] = StringUtils.defaultString(isbnAttributes.get("SOUSCATEGORIE2"));
        line[12] = exportSources(data);

        return line;
    }

    /**
     * Decrements the download counter, for instance when an IOException occurs (e.g., user stops the download).
     */
    public void decrementDownloadCounter() {
        concurrentDownloadsCounter.decrementAndGet();
    }

    /**
     * Provides the latest cached ISBN ZIP file size.
     *
     * @return the formatted size string.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public String isbnFileSize() {
        return humanReadableByteCountBin(openDataConfig.isbnZipFile().length());
    }

    /**
     * Provides the latest cached GTIN ZIP file size.
     *
     * @return the formatted size string.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public String gtinFileSize() {
        return humanReadableByteCountBin(openDataConfig.gtinZipFile().length());
    }

    /**
     * Returns the last update timestamp of the ISBN ZIP file.
     *
     * @return the update timestamp.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public Date isbnLastUpdate() {
        return Date.from(Instant.ofEpochMilli(openDataConfig.isbnZipFile().lastModified()));
    }

    /**
     * Returns the last update timestamp of the GTIN ZIP file.
     *
     * @return the update timestamp.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public Date gtinLastUpdate() {
        return Date.from(Instant.ofEpochMilli(openDataConfig.gtinZipFile().lastModified()));
    }

    /**
     * Counts the number of ISBN entries currently available in the repository.
     *
     * @return the entry count.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public long totalItemsISBN() {
        return aggregatedDataRepository.countItemsByBarcodeType(BarcodeType.ISBN_13);
    }

    /**
     * Counts the number of GTIN entries currently available in the repository.
     *
     * @return the entry count.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public long totalItemsGTIN() {
        return aggregatedDataRepository.countItemsByBarcodeType(
                BarcodeType.GTIN_8, BarcodeType.GTIN_12, BarcodeType.GTIN_13, BarcodeType.GTIN_14);
    }

    /**
     * Retrieves the total number of items in the main index.
     *
     * @return The total number of items.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public long totalItems() {
        return aggregatedDataRepository.countMainIndex();
    }

    /**
     * Converts a size in bytes to a human-readable format.
     *
     * @param bytes The size in bytes.
     * @return The size in a human-readable format (e.g., "1.2 MB").
     */
    public static String humanReadableByteCountBin(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }






}

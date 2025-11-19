package org.open4goods.services.opendata.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.open4goods.services.opendata.ThrottlingInputStream;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.util.concurrent.RateLimiter;

import io.micrometer.core.annotation.Timed;

/**
 * Service responsible for generating and managing the CSV OpenData files.
 * Handles the creation of GTIN and ISBN datasets, their compression into ZIP files,
 * and the management of download limits and rates.
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
            "min_price_compensation",
            "currency",
            "categories",
            "url"
    };

    public static final String[] ISBN_HEADER = {
            "isbn",
            "title",
            "last_updated",
            "offers_count",
            "min_price",
            "min_price_compensation",
            "currency",
            "url",
            "editeur",
            "format",
            "nb_page",
            "classification_decitre_1",
            "classification_decitre_2",
            "classification_decitre_3",
            "souscategorie",
            "souscategorie2"
    };

    private static final Set<String> ISBN_REQUIRED_FIELDS = Set.of(
            "id",
            "attributes",
            "gtinInfos",
            "offersCount",
            "price",
            "offerNames",
            "lastChange"
    );

    private static final Set<String> GTIN_REQUIRED_FIELDS = Set.of(
            "id",
            "attributes",
            "gtinInfos",
            "offersCount",
            "price",
            "offerNames",
            "lastChange",
            "datasourceCategories"
    );

    private static final List<String> ISBN_ATTRIBUTE_KEYS = List.of(
            "EDITEUR",
            "FORMAT",
            "NB DE PAGES",
            "CLASSIFICATION DECITRE 1",
            "CLASSIFICATION DECITRE 2",
            "CLASSIFICATION DECITRE 3",
            "SOUSCATEGORIE",
            "SOUSCATEGORIE2"
    );

    private final ProductRepository aggregatedDataRepository;
    private final OpenDataConfig openDataConfig;
    private final boolean generationEnabled;

    private final AtomicBoolean exportRunning = new AtomicBoolean(false);
    private final AtomicInteger concurrentDownloadsCounter = new AtomicInteger(0);

    public OpenDataService(ProductRepository aggregatedDataRepository,
                           OpenDataConfig openDataConfig) {
        Assert.notNull(aggregatedDataRepository, "aggregatedDataRepository must not be null");
        Assert.notNull(openDataConfig, "openDataConfig must not be null");
        Assert.notNull(openDataConfig.getGenerationEnabled(), "openDataConfig.generationEnabled must be configured");

        this.aggregatedDataRepository = aggregatedDataRepository;
        this.openDataConfig = openDataConfig;
        this.generationEnabled = openDataConfig.isGenerationEnabled();

        LOGGER.info("OpenDataService scheduled generation is {}", generationEnabled ? "enabled" : "disabled");
    }

    /**
     * This method checks the health status of the OpenDataService.
     * It verifies if the required ZIP files (ISBN and GTIN) exist.
     */
    @Override
    public Health health() {
        if (!openDataConfig.isbnZipFile().exists()) {
            return Health.down().withDetail("ISBN Zip File", "Le fichier ZIP ISBN est manquant.").build();
        }
        if (!openDataConfig.gtinZipFile().exists()) {
            return Health.down().withDetail("GTIN Zip File", "Le fichier ZIP GTIN est manquant.").build();
        }
        return Health.up().withDetail("OpenDataService", "Tous les fichiers ZIP sont présents.").build();
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
     * Generates the opendata CSV files and compresses them into ZIP files.
     * This method is scheduled to run periodically.
     */
    @Scheduled(initialDelay = 1000L * 3600 * 8, fixedDelay = 1000L * 3600 * 24 * 7)
    @Timed(value = "OpenDataService.generateOpendata.time", description = "Time taken to generate the OpenData ZIP files", extraTags = {"service", "OpenDataService"})
    public void generateOpendata() {

        if (!generationEnabled) {
            LOGGER.debug("Skipping OpenData generation because it is disabled by configuration");
            return;
        }

        if (exportRunning.getAndSet(true)) {
            LOGGER.error("Opendata export is already running");
            return;
        }

        try {
            prepareDirectories();
            processDataFiles();
            moveTmpFilesToFinalDestination();
            LOGGER.info("Opendata CSV files generated and zipped successfully.");
        } catch (Exception e) {
            LOGGER.error("Error while generating opendata set", e);
        } finally {
            exportRunning.set(false);
        }
    }

    /**
     * Prepares the necessary directories for storing temporary files.
     */
    private void prepareDirectories() throws IOException {
        File isbnFolder = openDataConfig.tmpIsbnZipFile().getParentFile();
        if (isbnFolder != null) {
            isbnFolder.mkdirs();
        }
        File gtinFolder = openDataConfig.tmpGtinZipFile().getParentFile();
        if (gtinFolder != null) {
            gtinFolder.mkdirs();
        }
    }

    /**
     * Processes and creates the ZIP files for the opendata.
     * ISBN and GTIN generations are executed in parallel to minimize overall runtime.
     */
    void processDataFiles() throws IOException {
        DatasetDefinition isbnDataset = new DatasetDefinition(
                ISBN_DATASET_FILENAME,
                openDataConfig.tmpIsbnZipFile(),
                ISBN_HEADER,
                Set.of(BarcodeType.ISBN_13),
                ISBN_REQUIRED_FIELDS,
                this::toIsbnEntry
        );

        DatasetDefinition gtinDataset = new DatasetDefinition(
                GTIN_DATASET_FILENAME,
                openDataConfig.tmpGtinZipFile(),
                GTIN_HEADER,
                Set.of(BarcodeType.GTIN_8, BarcodeType.GTIN_12, BarcodeType.GTIN_13, BarcodeType.GTIN_14),
                GTIN_REQUIRED_FIELDS,
                this::toGtinEntry
        );

        processDatasets(List.of(isbnDataset, gtinDataset));
    }

    /**
     * Moves the temporary files to their final destination.
     */
    private void moveTmpFilesToFinalDestination() throws IOException {
        moveFile(openDataConfig.tmpIsbnZipFile(), openDataConfig.isbnZipFile());
        moveFile(openDataConfig.tmpGtinZipFile(), openDataConfig.gtinZipFile());
    }

    /**
     * Moves a file from the source to the destination.
     */
    private void moveFile(File src, File dest) throws IOException {
        if (dest.exists()) {
            FileUtils.deleteQuietly(dest);
        }
        FileUtils.moveFile(src, dest);
    }

    private void processDatasets(List<DatasetDefinition> datasetDefinitions) throws IOException {
        try (DatasetWriterManager manager = new DatasetWriterManager(datasetDefinitions)) {
            writeDatasets(manager.writers(), datasetDefinitions);
            manager.writers().forEach(writer ->
                    LOGGER.info("{} rows exported in {}.", writer.count(), writer.definition().filename()));
        } catch (Exception e) {
            datasetDefinitions.forEach(definition -> FileUtils.deleteQuietly(definition.zipFile()));
            LOGGER.error("Error during processing of OpenData exports", e);
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unexpected error while generating OpenData exports", e);
        }
    }

    private void writeDatasets(List<DatasetWriter> writers, List<DatasetDefinition> datasetDefinitions) throws IOException {
        EnumSet<BarcodeType> barcodeFilters = EnumSet.noneOf(BarcodeType.class);
        datasetDefinitions.forEach(definition -> barcodeFilters.addAll(definition.barcodeTypes()));

        String[] includeFields = resolveIncludeFields(datasetDefinitions);
        EnumMap<BarcodeType, DatasetWriter> writerByType = mapWritersByType(writers);

        try (Stream<Product> stream = aggregatedDataRepository.exportAll(barcodeFilters, includeFields)) {
            stream.forEach(product -> {
                BarcodeType barcodeType = resolveBarcodeType(product);
                if (barcodeType == null) {
                    return;
                }
                DatasetWriter writer = writerByType.get(barcodeType);
                if (writer != null) {
                    writer.write(product);
                }
            });
        }
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

    private String[] resolveIncludeFields(List<DatasetDefinition> datasetDefinitions) {
        LinkedHashSet<String> includes = new LinkedHashSet<>();
        datasetDefinitions.forEach(definition -> includes.addAll(definition.requiredFields()));
        return includes.toArray(new String[0]);
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
     * Converts a Product object into a CSV row for GTIN.
     */
    private String[] toGtinEntry(Product data) {
        String[] line = new String[GTIN_HEADER.length];
        Arrays.fill(line, "");

        line[0] = StringUtils.defaultString(data.gtin());
        line[1] = StringUtils.defaultString(data.brand());
        line[2] = StringUtils.defaultString(data.model());
        line[3] = StringUtils.defaultString(data.shortestOfferName());

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
            if (bestPrice.getCompensation() != null) {
                line[9] = String.valueOf(bestPrice.getCompensation());
            }
            if (bestPrice.getCurrency() != null) {
                line[10] = bestPrice.getCurrency().toString();
            }
        }

        Collection<String> categories = data.getDatasourceCategories();
        if (categories != null && !categories.isEmpty()) {
            line[11] = StringUtils.join(categories, " ; ");
        }

        try {
            // TODO : Point to international website + internationalized URL
            String url = "https://nudger.fr/" + StringUtils.defaultString(data.gtin());
            line[12] = url;
        } catch (Exception e) {
            LOGGER.error("Error while extracting URL for GTIN {}", data.getId(), e);
        }

        return line;
    }

    /**
     * Converts a Product object into a CSV row for ISBN.
     */
    private String[] toIsbnEntry(Product data) {
        String[] line = new String[ISBN_HEADER.length];
        Arrays.fill(line, "");

        line[0] = StringUtils.defaultString(data.gtin());
        line[1] = StringUtils.defaultString(data.shortestOfferName());
        line[2] = String.valueOf(data.getLastChange());
        if (data.getOffersCount() != null) {
            line[3] = String.valueOf(data.getOffersCount());
        }

        AggregatedPrice bestPrice = data.bestPrice();
        if (bestPrice != null) {
            if (bestPrice.getPrice() != null) {
                line[4] = String.valueOf(bestPrice.getPrice());
            }
            if (bestPrice.getCompensation() != null) {
                line[5] = String.valueOf(bestPrice.getCompensation());
            }
            if (bestPrice.getCurrency() != null) {
                line[6] = bestPrice.getCurrency().toString();
            }
        }
        try {
            // TODO : Point to international website + internationalized URL
            String url = "https://nudger.fr/" + StringUtils.defaultString(data.gtin());
            line[7] = url;
        } catch (Exception e) {
            LOGGER.error("Error while extracting URL for ISBN {}", data.getId(), e);
        }

        Map<String, String> isbnAttributes = extractAttributes(data, ISBN_ATTRIBUTE_KEYS);
        line[8] = StringUtils.defaultString(isbnAttributes.get("EDITEUR"));
        line[9] = StringUtils.defaultString(isbnAttributes.get("FORMAT"));
        line[10] = StringUtils.defaultString(isbnAttributes.get("NB DE PAGES"));
        line[11] = StringUtils.defaultString(isbnAttributes.get("CLASSIFICATION DECITRE 1"));
        line[12] = StringUtils.defaultString(isbnAttributes.get("CLASSIFICATION DECITRE 2"));
        line[13] = StringUtils.defaultString(isbnAttributes.get("CLASSIFICATION DECITRE 3"));
        line[14] = StringUtils.defaultString(isbnAttributes.get("SOUSCATEGORIE"));
        line[15] = StringUtils.defaultString(isbnAttributes.get("SOUSCATEGORIE2"));

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

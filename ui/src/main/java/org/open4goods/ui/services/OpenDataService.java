package org.open4goods.ui.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.TechnicalException;
import org.open4goods.commons.helper.ThrottlingInputStream;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.product.IndexedAttribute;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.model.product.ProductAttribute;
import org.open4goods.model.BarcodeType;
import org.open4goods.ui.config.OpenDataConfig;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.util.concurrent.RateLimiter;
import com.opencsv.CSVWriter;

import io.micrometer.core.annotation.Timed;

/**
 * Service responsible for generating and managing the CSV opendata files.
 * Handles the creation of GTIN and ISBN datasets, their compression into ZIP files,
 * and the management of download limits and rates.
 */
public class OpenDataService implements HealthIndicator {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataService.class);

	private static final String ISBN_DATASET_FILENAME = "open4goods-isbn-dataset.csv";
	private static final String GTIN_DATASET_FILENAME = "open4goods-gtin-dataset.csv";

	private static final String[] GTIN_HEADER = {
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

	private static final String[] ISBN_HEADER = {
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

	private ProductRepository aggregatedDataRepository;
	private UiConfig uiConfig;
	private final OpenDataConfig openDataConfig;

	// The flag that indicates wether opendata export is running or not
	private AtomicBoolean exportRunning = new AtomicBoolean(false);
	private AtomicInteger concurrentDownloadsCounter = new AtomicInteger(0);


	public OpenDataService(ProductRepository aggregatedDataRepository, UiConfig uiConfig, OpenDataConfig openDataConfig){
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.uiConfig = uiConfig;
		this.openDataConfig = openDataConfig;
	}

	/**
	 * This method checks the health status of the OpenDataService.
	 * It verifies if the required ZIP files (ISBN and GTIN) exist.
	 */
	@Override
	public Health health() {
		if (!uiConfig.isbnZipFile().exists()) {
			return Health.down().withDetail("ISBN Zip File", "Le fichier ZIP ISBN est manquant.").build();
		}
		if (!uiConfig.gtinZipFile().exists()) {
			return Health.down().withDetail("GTIN Zip File", "Le fichier ZIP GTIN est manquant.").build();
		}
		return Health.up().withDetail("OpenDataService", "Tous les fichiers ZIP sont présents.").build();
	}

	/**
	 * Provides a limited bandwidth stream for downloading the opendata file.
	 * Enforces a limit on the number of concurrent downloads.
	 */
	public InputStream limitedRateStream() throws TechnicalException, FileNotFoundException {

		RateLimiter rateLimiter = RateLimiter.create(openDataConfig.getDownloadSpeedKb() * FileUtils.ONE_KB);

		if (concurrentDownloadsCounter.get() >= openDataConfig.getConcurrentDownloads()) {
			throw new TechnicalException("Too many requests ");
		} else {
			concurrentDownloadsCounter.incrementAndGet();
		}

		try {
			LOGGER.info("Starting opendata dataset download");
			return new ThrottlingInputStream(new BufferedInputStream(new FileInputStream(uiConfig.openDataFile())), rateLimiter) {
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
	 * TODO(p3,conf) : Schedule in conf
	 */
	@Scheduled(initialDelay = 1000L * 3600 * 8, fixedDelay = 1000L * 3600 * 24 * 7)
	@Timed(value = "OpenDataService.generateOpendata.time", description = "Time taken to generate the OpenData ZIP files", extraTags = {"service", "OpenDataService"})
	public void generateOpendata() {

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
		uiConfig.tmpIsbnZipFile().getParentFile().mkdirs();
		uiConfig.tmpGtinZipFile().getParentFile().mkdirs();
	}

	/**
	 * Processes and creates the ZIP files for the opendata.
	 */
	private void processDataFiles() throws IOException {
		LOGGER.info("Starting process for ISBN_13");
		processAndCreateZip(ISBN_DATASET_FILENAME, BarcodeType.ISBN_13, uiConfig.tmpIsbnZipFile());

		LOGGER.info("Starting process for GTIN/EAN");
		processAndCreateZip(GTIN_DATASET_FILENAME, BarcodeType.ISBN_13, uiConfig.tmpGtinZipFile(), true);
	}

	/**
	 * Moves the temporary files to their final destination.
	 */
	private void moveTmpFilesToFinalDestination() throws IOException {
		moveFile(uiConfig.tmpIsbnZipFile(), uiConfig.isbnZipFile());
		moveFile(uiConfig.tmpGtinZipFile(), uiConfig.gtinZipFile());
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

	private void processAndCreateZip(String filename, BarcodeType barcodeType, File zipFile) throws IOException {
		processAndCreateZip(filename, barcodeType, zipFile, false);
	}

	/**
	 * Processes the data and creates a ZIP file for the specified barcode type.
	 *
	 * @param filename    The name of the file to be created.
	 * @param barcodeType The type of barcode being processed.
	 * @param zipFile     The file object representing the ZIP file to be created.
	 * @param isGtinFile  Indicates if the file is a GTIN file.
	 * @throws IOException If there is an error during file creation or writing.
	 */
	private void processAndCreateZip(String filename, BarcodeType barcodeType, File zipFile, boolean isGtinFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(zipFile);
			 ZipOutputStream zos = new ZipOutputStream(fos);
			 CSVWriter writer = new CSVWriter(new OutputStreamWriter(zos))) {

			ZipEntry entry = new ZipEntry(filename);
			zos.putNextEntry(entry);

			// Set the appropriate header and barcode types
			BarcodeType[] types;
			if (isGtinFile) {
				writer.writeNext(GTIN_HEADER);
				types = new BarcodeType[]{BarcodeType.GTIN_8, BarcodeType.GTIN_12, BarcodeType.GTIN_13, BarcodeType.GTIN_14};
			} else {
				writer.writeNext(ISBN_HEADER);
				types = new BarcodeType[]{BarcodeType.ISBN_13};
			}

			AtomicLong count = new AtomicLong();

			aggregatedDataRepository.exportAll(types)
					.forEach(e -> {
						count.incrementAndGet();
						writer.writeNext(isGtinFile ? toGtinEntry(e) : toIsbnEntry(e));
					});

			writer.flush();
			zos.closeEntry();

			LOGGER.info("{} rows exported in {}.", count.get(), filename);

		} catch (Exception e) {
			LOGGER.error("Error during processing of {}: {}", filename, e.getMessage());
		}
	}



	/**
	 * Converts a Product object into a CSV row for GTIN.
	 */
	private String[] toGtinEntry(Product data) {
		String[] line = new String[GTIN_HEADER.length];

		line[0] = data.gtin(); // "code"
		line[1] = data.brand(); // "brand"
		line[2] = data.model(); // "model"
		line[3] = data.shortestOfferName(); // "name"
		
		
		line[4] = String.valueOf(data.getLastChange()); // "last_updated"
		line[5] = data.getGtinInfos().getCountry(); // "gs1_country"
		line[6] = data.getGtinInfos().getUpcType().toString(); // "gtinType"
		line[7] = String.valueOf(data.getOffersCount()); // "offers_count"

		if (null != data.bestPrice()) {
			line[8] = String.valueOf(data.bestPrice().getPrice()); // "min_price"
			line[9] = String.valueOf(data.bestPrice().getCompensation()); // "min_price_compensation"
			line[10] = data.bestPrice().getCurrency().toString(); // "currency"
		}
		line[11] = StringUtils.join(data.getDatasourceCategories(), " ; "); // "categories"

		try {
			// TODO(p3, i18N) : internationalize
			line[12] = uiConfig.getBaseUrl(Locale.FRANCE) + data.getNames().getUrl().get("fr");
		} catch (Exception e) {
			LOGGER.error("Errro while extracting URL for ISBN {}",data.getId());

		}


		return line;
	}

	/**
	 * Converts a Product object into a CSV row for ISBN.
	 */
	private String[] toIsbnEntry(Product data) {
		String[] line = new String[ISBN_HEADER.length];

		
//		"isbn",
//		"title",
//		"last_updated",
//		"offers_count",
//		"min_price",
//		"min_price_compensation",
//		"currency",
//		"url",
//		"editeur",
//		"format",
//		"nb_page",
//		"classification_decitre_1",
//		"classification_decitre_2",
//		"classification_decitre_3",
//		"souscategorie", 
//		"souscategorie2"
//		
//		
		line[0] = data.gtin(); // "code"
		line[1] = data.shortestOfferName(); // "name"
		line[2] = String.valueOf(data.getLastChange()); // "last_updated"
		line[3] = String.valueOf(data.getOffersCount()); // "offers_count"

		if (null != data.bestPrice()) {
			line[4] = String.valueOf(data.bestPrice().getPrice()); // "min_price"
			line[5] = String.valueOf(data.bestPrice().getCompensation()); // "min_price_compensation"
			line[6] = data.bestPrice().getCurrency().toString(); // "currency"
			try {
				// TODO(p3, i18N) : internationalize
				line[7] = uiConfig.getBaseUrl(Locale.FRANCE) + data.getNames().getUrl().get("fr");
			} catch (Exception e) {
				LOGGER.error("Errro while extracting URL for ISBN {}",data.getId());
			}
		}

		//TODO(p2, design) : We are looking for unormalized attributes. We should consider querying multiple  attributes in a failover mode
		// TODO(p3,conf) : Should be in config files
		line[8] = getAttribute(data, "EDITEUR");
		line[9] = getAttribute(data, "FORMAT");
		line[10] = getAttribute(data, "NB DE PAGES");
		line[11] = getAttribute(data, "CLASSIFICATION DECITRE 1");
		line[12] = getAttribute(data, "CLASSIFICATION DECITRE 2");
		line[13] = getAttribute(data, "CLASSIFICATION DECITRE 3");
		line[14] = getAttribute(data, "SOUSCATEGORIE");
		line[15] = getAttribute(data, "SOUSCATEGORIE2");

		return line;
	}

	/**
	 * Retrieves a specific attribute from the product data.
	 * TODO : Review when all attributes accessible by map
	 */
	private String getAttribute(Product data, String key) {

		String value = null;

		IndexedAttribute indexedAttr = data.getAttributes().getIndexed().get(key);
		
		
		if (null != indexedAttr) {
			value = indexedAttr.getValue();
		} else {
			ProductAttribute pAttr = data.getAttributes().getAll().get(key);
			if (null != pAttr) {
				value = pAttr.getValue();
			}
		}
		
		

		return value;
	}

	/**
	 * Decrements the download counter, for instance when an IOException occurs (e.g., user stops the download).
	 */
	public void decrementDownloadCounter() {
		concurrentDownloadsCounter.decrementAndGet();
	}

	@Cacheable(key = "#root.method.name + 'Isbn'", cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public String isbnFileSize() {
		return humanReadableByteCountBin(uiConfig.isbnZipFile().length());
	}

	@Cacheable(key = "#root.method.name + 'Gtin'", cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public String gtinFileSize() {
		return humanReadableByteCountBin(uiConfig.gtinZipFile().length());
	}

	@Cacheable(key = "#root.method.name + 'Isbn'", cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Date isbnLastUpdate() {
		return Date.from(Instant.ofEpochMilli(uiConfig.isbnZipFile().lastModified()));
	}

	@Cacheable(key = "#root.method.name + 'Gtin'", cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Date gtinLastUpdate() {
		return Date.from(Instant.ofEpochMilli(uiConfig.gtinZipFile().lastModified()));
	}

	@Cacheable(key = "#root.method.name + 'Isbn'", cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
	public long totalItemsISBN() {
		return aggregatedDataRepository.countItemsByBarcodeType(BarcodeType.ISBN_13);
	}

	@Cacheable(key = "#root.method.name + 'Gtin'", cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
	public long totalItemsGTIN() {
		return aggregatedDataRepository.countItemsByBarcodeType(
				BarcodeType.GTIN_8, BarcodeType.GTIN_12, BarcodeType.GTIN_13, BarcodeType.GTIN_14);
	}

	/**
	 * Retrieves the total number of items in the main index.
	 *
	 * @return The total number of items.
	 */
	@Cacheable(key = "#root.method.name", cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
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
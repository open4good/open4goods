package org.open4goods.ui.services;

import java.io.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.TechnicalException;
import org.open4goods.commons.helper.ThrottlingInputStream;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.product.Product;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.util.concurrent.RateLimiter;
import com.opencsv.CSVWriter;

/**
 * Service in charge of generating the CSV opendata file
 *
 * @author Goulven.Furet
 *
 */
public class OpenDataService {

	// Allowed download speed in kb
	private static final int DOWNLOAD_SPEED_KB = 256;

	public static final int CONCURRENT_DOWNLOADS = 4;

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataService.class);

	// The headers
	private static final String[] header = { "code", "brand", "model", "name", "last_updated", "gs1_country", "gtinType",
			"offers_count", "min_price", "min_price_compensation", "currency", "categories", "url" };

	private ProductRepository aggregatedDataRepository;
	private UiConfig uiConfig;

	// The flag that indicates wether opendata export is running or not
	private AtomicBoolean exportRunning = new AtomicBoolean(false);

	private AtomicInteger concurrentDownloads = new AtomicInteger(0);

	private static final String ISBN_DATASET_FILENAME = "open4goods-isbn-dataset.csv";
	private static final String GTIN_DATASET_FILENAME = "open4goods-gtin-dataset.csv";

	@Autowired
	public OpenDataService(ProductRepository aggregatedDataRepository, UiConfig uiConfig){
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.uiConfig = uiConfig;
	}

	/**
	 *
	 * @return a limited stream pageSize the opendata set. Limited in bandwith, and
	 *         limited in number of conccurent downloads Limited
	 * @throws FileNotFoundException
	 * @throws TechnicalException
	 */
	public InputStream limitedRateStream() throws TechnicalException, FileNotFoundException {

		// TODO : in conf
		RateLimiter rateLimiter = RateLimiter.create(DOWNLOAD_SPEED_KB * FileUtils.ONE_KB);

		// TODO : in conf
		if (concurrentDownloads.get() >= CONCURRENT_DOWNLOADS) {
			throw new TechnicalException("Too many requests ");
		} else {
			concurrentDownloads.incrementAndGet();
		}

		try {
			LOGGER.info("Starting opendata dataset download");
			return new ThrottlingInputStream(new BufferedInputStream(new FileInputStream(uiConfig.openDataFile())),
					rateLimiter) {
				@Override
				public void close() throws IOException {
					super.close();
					concurrentDownloads.decrementAndGet();
					LOGGER.info("Ending opendata dataset download");
				}
			};
		} catch (IOException e) {
			concurrentDownloads.decrementAndGet();
			throw e;
		}
	}

	/**
	 * Iterates over all aggregated data to generate the zipped opendata CSV file.
	 *
	 * TODO : Schedule in conf
	 */
	@Scheduled(initialDelay = 1000L *3600, fixedDelay = 1000L * 3600 * 24 * 7)
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

	private void processAndCreateZip(String filename, BarcodeType barcodeType, File zipFile) throws IOException {
		processAndCreateZip(filename, barcodeType, zipFile, false);
	}

	private void prepareDirectories() throws IOException {
		uiConfig.tmpIsbnZipFile().getParentFile().mkdirs();
		uiConfig.tmpGtinZipFile().getParentFile().mkdirs();
	}

	private void processDataFiles() throws IOException {
		LOGGER.info("Starting process for ISBN_13");
		processAndCreateZip(ISBN_DATASET_FILENAME, BarcodeType.ISBN_13, uiConfig.tmpIsbnZipFile());

		LOGGER.info("Starting process for GTIN/EAN");
		processAndCreateZip(GTIN_DATASET_FILENAME, BarcodeType.ISBN_13, uiConfig.tmpGtinZipFile(), true);
	}

	private void moveTmpFilesToFinalDestination() throws IOException {
		moveFile(uiConfig.tmpIsbnZipFile(), uiConfig.isbnZipFile());
		moveFile(uiConfig.tmpGtinZipFile(), uiConfig.gtinZipFile());
	}

	private void moveFile(File src, File dest) throws IOException {
		if (dest.exists()) {
			FileUtils.deleteQuietly(dest);
		}
		FileUtils.moveFile(src, dest);
	}

	private void processAndCreateZip(String filename, BarcodeType barcodeType, File zipFile, boolean invertCondition) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(zipFile);
			 ZipOutputStream zos = new ZipOutputStream(fos);
			 CSVWriter writer = new CSVWriter(new OutputStreamWriter(zos))) {

			ZipEntry entry = new ZipEntry(filename);
			zos.putNextEntry(entry);
			writer.writeNext(header);

			AtomicLong count = new AtomicLong();
			aggregatedDataRepository.exportAll().limit(500).filter(e ->
					invertCondition ? !e.getGtinInfos().getUpcType().equals(barcodeType) : e.getGtinInfos().getUpcType().equals(barcodeType)
			).forEach(e -> {
				count.incrementAndGet();
				writer.writeNext(toEntry(e));
			});
			writer.flush();
			zos.closeEntry(); // Ensure the entry is closed before ending the try block

			LOGGER.info("{} rows exported in {}.", count.get(), filename);

		} catch (Exception e) {
			LOGGER.error("Error during processing of {}: {}", filename, e.getMessage());
		}
	}

	/**
	 * Convert an aggregateddata pageSize a csv row
	 *
	 * @param data
	 * @return
	 */
	private String[] toEntry(Product data) {

		String[] line = new String[header.length];

		//		"gtin"
		line[0] = data.gtin();
		//		"brand"
		line[1] = data.brand();
		//		"model"
		line[2] = data.model();
		//		"shortest_name"
		line[3] = data.shortestOfferName();
		//		"last_updated"
		line[4] = String.valueOf(data.getLastChange());
		//		"gs1_country"
		line[5] = data.getGtinInfos().getCountry();
		//		"upcType"
		line[6] = data.getGtinInfos().getUpcType().toString();


		//		"offers_count"
		line[7] = String.valueOf(data.getOffersCount());
		//		"min_price"
		if (null != data.bestPrice()) {
			line[8] = String.valueOf(data.bestPrice().getPrice());
			// "compensation"
			line[9] = String.valueOf(data.bestPrice().getCompensation());
			// "currency"
			line[10] = data.bestPrice().getCurrency().toString();
			// "url"
			// TODO(gof) : i18n the url
			line[12] = ""; //uiConfig.getBaseUrl(Locale.FRANCE) + data.getNames().getName();
		}

		// Categories
		line[11] = StringUtils.join(data.getDatasourceCategories()," ; ");

		return line;
	}

	/**
	 * Used pageSize decrement the download counter, for instance when IOexception occurs
	 * (user stop the download)
	 */
	public void decrementDownloadCounter() {
		concurrentDownloads.decrementAndGet();
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

	/**
	 *
	 * @return number of items
	 */
	@Cacheable(key = "#root.method.name", cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
	public long totalItems() {
		return aggregatedDataRepository.countMainIndex();
	}

	/**
	 * Convert a size in human readable form
	 * @param bytes
	 * @return
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

package org.open4goods.api.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.model.product.Product;
import org.open4goods.services.SerialisationService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;

import io.micrometer.core.annotation.Timed;

/**
 * Service in charge of backuping open4gods data
 * 
 * @author Goulven.Furet
 *
 *
 */

public class BackupService implements HealthIndicator {

	protected static final Logger logger = LoggerFactory.getLogger(BackupService.class);

	// TODO : Consts from conf

	private static final int MIN_XWIKI_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 15;
	
	private static final long MIN_PRODUCT_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 200;

	private static final int DATA_EXPORT_THREADS_COUNT = 8;

	// Cron period + 2h
	private static final long MAX_WIKI_BACKUP_AGE = 1000 * 3600 * 14;
	// Cron period + 1d
	private static final long MAX_PRODUCTS_BACKUP_AGE = 1000 * 3600 * 24 * 8;

	private AtomicBoolean wikiExportRunning = new AtomicBoolean(false);
	private AtomicBoolean productExportRunning = new AtomicBoolean(false);

	private XWikiReadService xwikiService;
	private ProductRepository productRepo;
	private SerialisationService serialisationService;

	private BackupConfig backupConfig;

	// Used to trigger Health.down() if exception occurs
	private String wikiException;
	private String dataBackupException;

	
	 
	public BackupService(XWikiReadService xwikiService, ProductRepository productRepo, BackupConfig backupConfig, SerialisationService serialisationService) {
		super();
		this.xwikiService = xwikiService;
		this.productRepo = productRepo;
		this.backupConfig = backupConfig;
		this.serialisationService = serialisationService;
	}

	/**
	 * This method will periodicaly export all data in a zipped file
	 */
	// TODO : Schedule from conf
	@Scheduled(initialDelay = 1000 * 3600 * 6, fixedDelay = 1000 * 3600 * 24 * 7)
	@Timed(value = "backup.products", description = "Backup of all products", extraTags = { "service" })
	   public void backupProducts() {
        logger.info("Products data backup - start");
   	 	ExecutorService executorService = Executors.newFixedThreadPool(DATA_EXPORT_THREADS_COUNT);

   	 
        // Checking not already running
        if (productExportRunning.get()) {
            logger.warn("Product export is already running. Skipped");
            return;
        } else {
            productExportRunning.set(true);
        }

        File tmp = null;
        try {
            // Creating parent folders if necessary
            File parent = new File(backupConfig.getDataBackupFile()).getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            // Creating tmp file
            tmp = File.createTempFile("product-backup", ".zip");

            try (FileOutputStream fos = new FileOutputStream(tmp);
                 GZIPOutputStream zos = new GZIPOutputStream(fos);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"))) {

                // Parallel processing of JSON serialization and compression
                productRepo.exportAll()
                    .map(product -> executorService.submit(() -> serializeAndWriteProduct(product, writer)))
                    .forEach(this::waitForCompletion);

            } catch (IOException e) {
                logger.error("Error during backup : {}", e.getMessage());
                throw new UncheckedIOException(e);
            }

            // Checking tmp file exists and is not empty
            if (!tmp.exists() || tmp.length() < MIN_PRODUCT_BACKUP_SIZE_IN_BYTES) {
                throw new Exception("Empty tmp product backup file");
            }

            // Move to target file
            Files.move(tmp.toPath(), Path.of(backupConfig.getDataBackupFile()), StandardCopyOption.REPLACE_EXISTING);

            // Unsetting previous exception if any
            this.dataBackupException = null;
        } catch (Exception e) {
            logger.error("Error while backing up data", e);
            this.dataBackupException = e.getMessage();
        } finally {
            // Deleting tmp file, if exists
            if (tmp != null && tmp.exists()) {
                FileUtils.deleteQuietly(tmp);
            }
            productExportRunning.set(false);
            executorService.shutdown();
        }

        logger.info("Products data backup - complete");
    }

	/**
	 * Import product from the GZIP file
	 * @throws Exception 
	 */
	public void importProducts() {
	    logger.info("Product import : started");

	    File importFile = new File(backupConfig.getImportProductPath());
	    if (!importFile.exists()) {
	        logger.error("Import file does not exists : {}", importFile.getAbsolutePath());
	        return;
	    }

	    try (InputStream inputStream = new FileInputStream(importFile);
	         GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
	         InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
	         BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

	        List<Product> group = new ArrayList<>();
	        
	        bufferedReader.lines().forEach(line -> {
	            try {
					group.add(serialisationService.fromJson(line, Product.class));
				} catch (IOException e) {
					logger.error("Error occurs in data deserialisation", e);
				}
	            if (group.size() == 200) {
	                productRepo.storeNoCache(group); // Index the current group
	                group.clear(); // Clear the group for the next batch
	            }
	        });

	        // Index the remaining lines if any
	        if (!group.isEmpty()) {
	            productRepo.storeNoCache(group);
	        }

	    } catch (Exception e) {
	        logger.error("Error occurs in data file import", e);
	    }
	    logger.info("Product import : finished");
	}
	
	
    private void serializeAndWriteProduct(Object product, BufferedWriter writer) {
        String json = serialisationService.toJson(product);
        synchronized (writer) {
            try {
                writer.write(json);
                writer.newLine(); // To separate JSON objects
            } catch (IOException e) {
                logger.error("Error writing JSON data to backup file", e);
                throw new UncheckedIOException(e);
            }
        }
    }

    private void waitForCompletion(Future<?> future) {
        try {
            future.get(); // Waits for the task to complete
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error during parallel processing", e);
            throw new RuntimeException(e);
        }
    }

	/**
	 * This method will periodicaly backup the Xwiki content
	 */
	// TODO : Schedule from conf
	@Scheduled(initialDelay = 1000 * 3600, fixedDelay = 1000 * 3600 * 12)
	@Timed(value = "backup.wiki", description = "Backup of all the xwiki content", extraTags = { "service" })
	public void backupXwiki() {

		// Checking not already running
		if (wikiExportRunning.get()) {
			logger.warn("Xwiki export is already running. Skipped.");
			return;
		} else {
			wikiExportRunning.set(true);
		}

		logger.info("Xwiki backup - start");
		try {
			// Creating parent folders if necessary
			File parent = new File(backupConfig.getXwikiBackupFile()).getParentFile();
			parent.mkdirs();

			// Creating tmp file
			File tmp = File.createTempFile("xwiki-backup", "backup");

			// Exporting to tmp file
			xwikiService.exportXwikiContent(tmp);

			// Checking tmp file exists and not empty
			if (!tmp.exists() || tmp.length() < MIN_XWIKI_BACKUP_SIZE_IN_BYTES) {
				throw new Exception("Empty or not large enough tmp xwiki backup file");
			}

			// Move to target file
		    Files.move(tmp.toPath(), Path.of(backupConfig.getXwikiBackupFile()), StandardCopyOption.REPLACE_EXISTING);

		    this.wikiException = null;

		} catch (Exception e) {
			logger.error("Error while backuping Xwiki", e);
			this.wikiException = e.getMessage();
		} finally {
		}

		wikiExportRunning.set(false);
		logger.info("Xwiki backup - finished");
	}

	/**
	 * Health Check computing
	 */
	@Override
	public Health health() {

		Map<String, String> errorMessages = new HashMap<>();

		/////////////////////////////
		// Xwiki file check
		/////////////////////////////

		File wikiFile = new File(backupConfig.getXwikiBackupFile());
		// Check exceptions during processing
		if (null != wikiException) {
			errorMessages.put("xwiki_export_exception", wikiException);
		}

		// Check exists
		if (!Files.exists(Path.of(backupConfig.getXwikiBackupFile()))) {
			errorMessages.put("xwiki_backup_missing", backupConfig.getXwikiBackupFile());
		}

		// Check minimum size
		if (wikiFile.length() < MIN_XWIKI_BACKUP_SIZE_IN_BYTES) {
			errorMessages.put("xwiki_backup_min_size", String.valueOf(wikiFile.length()));
		}

		// Check date is not to old
		// NOTE : In the best world, MAX_WIKI_BACKUP_AGE would be derivated from the
		// schedule rate
		if (System.currentTimeMillis() - wikiFile.lastModified() > MAX_WIKI_BACKUP_AGE) {
			errorMessages.put("xwiki_backup_too_old", String.valueOf(wikiFile.lastModified()));
		}

		/////////////////////////////
		// Products backup file check
		/////////////////////////////

		File productFile = new File(backupConfig.getDataBackupFile());

		// Check exceptions during processing
		if (null != dataBackupException) {
			errorMessages.put("product_export_exception", dataBackupException);
		}

		// Check exists
		if (!Files.exists(Path.of(backupConfig.getDataBackupFile()))) {
			errorMessages.put("product_backup_missing", backupConfig.getDataBackupFile());
		}

		// Check minimum size
		if (productFile.length() < MIN_PRODUCT_BACKUP_SIZE_IN_BYTES) {
			errorMessages.put("product_backup_min_size", String.valueOf(productFile.length()));
		}

		// Check date is not to old
		// NOTE : In the best world, MAX_WIKI_PRODUCT_AGE would be derivated from the
		// schedule rate
		// TODO : Check
		if (System.currentTimeMillis() - productFile.lastModified() > MAX_PRODUCTS_BACKUP_AGE) {
			errorMessages.put("product_backup_too_old", String.valueOf(productFile.lastModified()));
		}

		// Building the healthcheck

		Health health = null;

		if (errorMessages.size() == 0) {
			// All is fine
			health = Health.status(Status.UP).withDetail(this.getClass().getSimpleName(), "Backups are OK").build();
		} else {
			health = Health.down().withDetails(errorMessages).build();
		}

		return health;
	}

}

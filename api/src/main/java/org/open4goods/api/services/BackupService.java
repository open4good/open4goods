package org.open4goods.api.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.dao.ProductRepository;
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
	private static final long MIN_PRODUCT_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 2000;
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
			     ZipOutputStream zos = new ZipOutputStream(fos);
			     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"))) {

			    // Create a new zip entry for the backup
			    ZipEntry zipEntry = new ZipEntry("product-backup.json");
			    zos.putNextEntry(zipEntry);

			    // Stream JSON objects into the zip file
			    productRepo.exportAll().forEach(data -> {
			        String json = serialisationService.toJson(data);
			        try {
			            writer.write(json);
			            writer.newLine(); // To separate JSON objects
			        } catch (IOException e) {
			            logger.error("Error writing JSON data to backup file", e);
			            throw new UncheckedIOException(e);
			        }
			    });

			    // Close the zip entry
			    zos.closeEntry();
			} catch (IOException e) {
			    logger.error("Error during backup : {}", e.getMessage());
			    throw new UncheckedIOException(e);
			}
			
			
			// Checking tmp file exists and not empty
			if (!tmp.exists() || tmp.length() < MIN_PRODUCT_BACKUP_SIZE_IN_BYTES) {
				throw new Exception("Empty tmp produt backup file");
			}
			
			// Move to target file
		    Files.move(tmp.toPath(), Path.of(backupConfig.getDataBackupFile()), StandardCopyOption.REPLACE_EXISTING);
		    
		    // Unsetting previous exception if any
		    this.dataBackupException = null;
		} catch (Exception e) {
			logger.error("Error while backuping datas",e);
			this.dataBackupException = e.getMessage();
		} finally {
			// Deleting tmp file, if exists
			if (null != tmp && tmp.exists()) {
				FileUtils.deleteQuietly(tmp);
			}
		}

		productExportRunning.set(false);
		logger.info("Products data backup - complete");
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
		if (System.currentTimeMillis() - wikiFile.lastModified() < MAX_WIKI_BACKUP_AGE) {
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
		if (System.currentTimeMillis() - productFile.lastModified() < MAX_PRODUCTS_BACKUP_AGE) {
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

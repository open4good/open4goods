package org.open4goods.api.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.SerialisationService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;

import io.micrometer.core.annotation.Timed;

/**
 * Service in charge of backuping open4gods data 
 * 
 * @author Goulven.Furet
 *
 *
 */
public class BackupService {

	
	private static final int MIN_XWIKI_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 10;
	private static final long MIN_PRODUCT_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 100;

	protected static final Logger logger = LoggerFactory.getLogger(BackupService.class);

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
	@Scheduled(initialDelay = 1000 * 3600 * 6 , fixedDelay = 1000 * 3600 * 48)
	@Timed(value = "backup.products", description = "Backup of all products", extraTags = {"service"})
	public void backupProducts() {
	    logger.info("Products data backup - start");

	    try {
			// Creating parent folders if necessary
			File parent = new File(backupConfig.getDataBackupFile()).getParentFile();
			if (!parent.exists()) {
			    parent.mkdirs();
			}

			// Creating tmp file
			File tmp = File.createTempFile("product-backup", ".zip");

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
			
			// Copy to target file
			File dest = new File(backupConfig.getXwikiBackupFile());
			// Deleting previous one
			Files.delete(dest.toPath());
			
			// Moving tmp file to dest file 
			Files.move(tmp.toPath(), dest.toPath());
			
			
			
		} catch (Exception e) {
			logger.error("Error while backuping datas",e);
			this.dataBackupException = e.getMessage();
		} finally {
			this.dataBackupException = null;
		}

	    logger.info("Products data backup - complete");
	}
	
	
	
	/**
	 * This method will periodicaly backup the Xwiki content
	 */
	// TODO : Schedule from conf
	@Scheduled(initialDelay = 1000 * 3600, fixedDelay = 1000 * 3600 * 12)
	@Timed(value = "backup.wiki", description = "Backup of all the xwiki content", extraTags = {"service"})
	public void backupXwiki () {
		
		logger.info("Xiki backup - start");
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
				throw new Exception("Empty tmp xwiki backup file");
			}
			
			// Copy to target file
			File dest = new File(backupConfig.getXwikiBackupFile());
			// Deleting previous one
			Files.delete(dest.toPath());
			
			// Moving tmp file to dest file 
			Files.move(tmp.toPath(), dest.toPath());
			
		} catch (Exception e) {
			logger.error("Error while backuping Xwiki",e);
			this.wikiException = e.getMessage();
		} finally {	
			this.wikiException = null;
		}
		logger.info("Xiki backup - finished");

	}
	
	
	

}

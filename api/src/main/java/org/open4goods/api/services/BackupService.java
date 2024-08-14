package org.open4goods.api.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

	
	private static final int MIN_XWIKI_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 10;
	private static final long MIN_PRODUCT_BACKUP_SIZE_IN_BYTES = 1024 * 1024 * 100;
	private static final int DATA_EXPORT_THREADS_COUNT = 6;

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
			File tmp = File.createTempFile("products-backup", ".zip");

			// BlockingQueue to store serialized JSON strings
			BlockingQueue<String> queue = new LinkedBlockingQueue<>();

			// ExecutorService for parallel serialization
			ExecutorService serializationExecutor = Executors.newFixedThreadPool(DATA_EXPORT_THREADS_COUNT);

			// Flag to signal completion of serialization
			CountDownLatch latch = new CountDownLatch(1);

			// Writer thread to write JSON strings to the zip file
			Thread writerThread = new Thread(() -> {
			    try (FileOutputStream fos = new FileOutputStream(tmp);
			         ZipOutputStream zos = new ZipOutputStream(fos);
			         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"))) {

			        // Create a new zip entry for the backup
			        ZipEntry zipEntry = new ZipEntry("products.json");
			        zos.putNextEntry(zipEntry);

			        // Write JSON strings from the queue to the file
			        while (latch.getCount() > 0 || !queue.isEmpty()) {
			            String json = queue.poll(1, TimeUnit.SECONDS);
			            if (json != null) {
			                writer.write(json);
			                writer.newLine();
			            }
			        }

			        // Close the zip entry
			        zos.closeEntry();
			    } catch (IOException | InterruptedException e) {
			        logger.error("Error writing JSON data to backup file", e);
			        throw new UncheckedIOException(new IOException(e));
			    }
			});

			// Start the writer thread
			writerThread.start();

			// Parallel serialization of products
			productRepo.exportAll()
			// TODO : Remove limit
				.limit(100000)
				.forEach(data -> {
			    serializationExecutor.submit(() -> {
			        String json = serialisationService.toJson(data);
			        try {
			            queue.put(json);
			        } catch (InterruptedException e) {
			            Thread.currentThread().interrupt();
			            logger.error("Interrupted while adding JSON to queue", e);
			        }
			    });
			});

			// Shut down serialization executor and signal the writer thread to finish
			serializationExecutor.shutdown();
			try {
			    if (serializationExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
			        latch.countDown(); // Signal that serialization is complete
			        writerThread.join(); // Wait for writer thread to finish
			    }
			} catch (InterruptedException e) {
			    Thread.currentThread().interrupt();
			    throw e;
			}
		} catch (Exception e) {
			logger.error("Error while exporting datas",e);
			this.dataBackupException = e.getMessage();
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




		@Override
	    public Health health() {
	       
			Health health1 = Health.status(Status.DOWN).build();
			
			Health health2 = Health.status(Status.DOWN).build();
			
			
			return null; // TODO : Aggregate the health1 
	    }



}

package org.open4goods.api.services.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

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



	private XWikiReadService xwikiService;
	private ProductRepository productRepo;
	private SerialisationService serialisationService;

	private BackupConfig backupConfig;

	// Used to trigger Health.down() if exception occurs
	private String wikiException;
	private String dataBackupException;

	// Count of last exported items
	private AtomicLong expordedProductsCounter = new AtomicLong(0L);
	private AtomicLong expectedBackupedProducts = new AtomicLong(0L);
	
	// Flags to avoid conccurent export running
	private AtomicBoolean wikiExportRunning = new AtomicBoolean(false);
	private AtomicBoolean productExportRunning = new AtomicBoolean(false);

	
	
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

        // The bloking queue, used to share the items to be backuped with threads
        LinkedBlockingQueue<Product> blockingQueue = new LinkedBlockingQueue<Product>(1000);
        // Reset the counter item flags
        expordedProductsCounter.set(0L);
        
        try {
	        ExecutorService executorService = Executors.newFixedThreadPool(backupConfig.getProductsExportThreads());
	        
	        // Starting files writing threads
	        File backupFolder = new File(backupConfig.getDataBackupFolder());
	        
	        if (backupFolder != null && backupFolder.exists() && !backupFolder.isDirectory()) {
	        	throw new Exception("is a file");
	        } else  if (backupFolder != null && !backupFolder.exists()) {
	            if (!backupFolder.mkdirs()) {
	                throw new IOException("Failed to create parent directories: " + backupFolder.getAbsolutePath());
	            }
	        }
	        
	        // Creating the folders if needed
	        backupFolder.mkdirs();
	        
	        // Starting the files writing threads
	        for (int i = 0; i < backupConfig.getProductsExportThreads(); i++) {
	        	executorService.submit(new ProductBackupThread(backupFolder, blockingQueue, serialisationService, i));
	        }
	        
	        
	        // Setting the expected number of items (min, new items can be indexed during backup)
	        expectedBackupedProducts.set(productRepo.countMainIndex());
	        
	        // Exporting all datas to the blocking queue
	        productRepo.exportAll()
	        .forEach(e -> {
	        	try {
	        		// Incrementing the counter
	        		expordedProductsCounter.incrementAndGet();
	        		
					blockingQueue.offer(e, 10, TimeUnit.HOURS);
				} catch (InterruptedException e1) {
		            logger.error("Error while backing up data", e);
		            this.dataBackupException = e1.getMessage();
				}
	        });
	        
	        executorService.shutdown();
        	
        } catch (Exception e) {
            logger.error("Error while backing up data", e);
            this.dataBackupException = e.getMessage();
        } finally {
            productExportRunning.set(false);
        }

        logger.info("Products data backup - complete");
    }

	/**
	 * Import product from the GZIP files
	 * NOTE : Could increase perf by having one import thread per file
	 * @throws Exception 
	 */
	public void importProducts() {
	    logger.info("Product import : started");

	    File importFolder = new File(backupConfig.getImportProductPath());
	    if (!importFolder.exists() || !importFolder.isDirectory()) {
	        logger.error("Import file does not exists or is not a folder : {}", importFolder.getAbsolutePath());
	        return;
	    }

	    for (File importFile : importFolder.listFiles()) {
	    	logger.info("Importing file started : {}", importFile.getAbsolutePath());
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
		            if (group.size() == backupConfig.getImportBulkSize()) {
		                productRepo.storeNoCache(group); // Index the current group
		                group.clear(); // Clear the group for the next batch
		            }
		        });
	
		        // Index the remaining lines if any
		        if (!group.isEmpty()) {
		            productRepo.storeNoCache(group);
		        }
		    	logger.info("Importing file finished : {}", importFile.getAbsolutePath());
		    } catch (Exception e) {
		        logger.error("Error occurs in data file import", e);
		    }
	    }
	    logger.info("Product import : finished");
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
			if (!tmp.exists() || tmp.length() < backupConfig.getMinXwikiBackupFileSizeInMb() * 1024 * 1024) {
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
		long wikiLastModified = wikiFile.lastModified();
		long wikiFileSize = wikiFile.length();
		
		
		// Check exceptions during processing
		if (null != wikiException) {
			errorMessages.put("xwiki_export_exception", wikiException);
		}

		// Check exists
		if (!Files.exists(Path.of(backupConfig.getXwikiBackupFile()))) {
			errorMessages.put("xwiki_backup_missing", backupConfig.getXwikiBackupFile());
		}

		// Check minimum size
		if (wikiFileSize < backupConfig.getMinXwikiBackupFileSizeInMb() * 1024L * 1024L) {
			errorMessages.put("xwiki_backup_size_too_small", FileUtils.byteCountToDisplaySize(wikiFileSize) + " < " + backupConfig.getMinXwikiBackupFileSizeInMb() + " Mb");
		}

		// Check date is not to old
		// NOTE : In the best world, MAX_WIKI_BACKUP_AGE would be derivated from the
		// schedule rate
		if (System.currentTimeMillis() - wikiLastModified > backupConfig.getMaxWikiBackupAgeInHours() * 3600 * 1000) {
			errorMessages.put("xwiki_backup_too_old", String.valueOf(wikiLastModified));
		}

		/////////////////////////////
		// Products backup files check
		/////////////////////////////

		File productFolder = new File(backupConfig.getDataBackupFolder());
		long productLastModified = productFolder.lastModified();
		long productFolderSize = FileUtils.sizeOfDirectoryAsBigInteger(productFolder).longValue();
		
		
		// Check we have the expected number of items backuped (only if not running)
		if (!productExportRunning.get()) {
			if (expordedProductsCounter.longValue() < expectedBackupedProducts.longValue()) {
				errorMessages.put("product_exported_items_too_low",  expordedProductsCounter.longValue() + " < " + expectedBackupedProducts.longValue());
			}
		}
		
		// Check exceptions during processing
		if (null != dataBackupException) {
			errorMessages.put("product_export_exception", dataBackupException);
		}

		// Check exists
		// TODO
		if (!Files.exists(Path.of(backupConfig.getDataBackupFolder()))) {
			errorMessages.put("product_backup_missing", backupConfig.getDataBackupFolder());
		}

		// Check minimum size
		if (productFolderSize <  backupConfig.getMinProductsBackupFolderSizeInMb() * 1024L * 1024L) {
			errorMessages.put("product_backup_size_too_small", FileUtils.byteCountToDisplaySize(productFolderSize) + " < " +  backupConfig.getMinProductsBackupFolderSizeInMb() + " Mb");
		}

		// Check date is not to old
		// NOTE : In the best world, MAX_WIKI_PRODUCT_AGE would be derivated from the schedule rate

		long oldestFileTs = Long.MAX_VALUE;
		// TODO : Check count
		File[] productBackupFiles = productFolder.listFiles();
		
		/*
		 * Checking number of files is correct
		 */
		if (productBackupFiles.length != backupConfig.getProductsExportThreads()) {
			errorMessages.put("product_backup_file_count", productBackupFiles.length + " <> " +  backupConfig.getProductsExportThreads());
		}
		
		for (File f : productBackupFiles) {
			if (f.lastModified() < oldestFileTs) {
				oldestFileTs = f.lastModified();
			}
		}
	
		/**
		 * Checking product backup oldness
		 */
		if (System.currentTimeMillis() - oldestFileTs > backupConfig.getMaxProductsBackupAgeInHours() * 3600L * 1000L) {
			errorMessages.put("product_backup_too_old", new Date (productLastModified).toString());
		}

		// Building the healthcheck

		Health health = null;

		if (errorMessages.size() == 0) {
			// All is fine
			health = Health.status(Status.UP)
					.withDetail("product_export_running", productExportRunning.getClass())
					.withDetail("product_backup_date", new Date (productLastModified).toString())
					.withDetail("product_backup_size", FileUtils.byteCountToDisplaySize(productFolderSize))
					.withDetail("product_exported_items",  expordedProductsCounter.longValue())
					
					.withDetail("xwiki_export_running", wikiExportRunning.getClass())
					.withDetail("xwiki_backup_date", new Date (wikiLastModified).toString())
					.withDetail("xwiki_backup_size", FileUtils.byteCountToDisplaySize(wikiFileSize))
					.build();
		} else {
			health = Health.down().withDetails(errorMessages).build();
		}

		return health;
	}

}
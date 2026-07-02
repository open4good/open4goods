package org.open4goods.api.services.backup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.backup.ProductBackupThread.ProductBackupFile;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	private static final String PRODUCT_BACKUP_FILE_PREFIX = "products-backup-";
	private static final String PRODUCT_BACKUP_FILE_SUFFIX = ".gz";
	private static final String PRODUCT_BACKUP_MANIFEST = "products-backup-manifest.json";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();



	private XWikiReadService xwikiService;
	private ProductRepository productRepo;
	private SerialisationService serialisationService;

	private AggregationFacadeService aggregationService;

	private BackupConfig backupConfig;

	// Used to trigger Health.down() if exception occurs
	private String wikiException;
	private String dataBackupException;
	private String dataCopyException;

	// Count of last exported items
	private AtomicLong expordedProductsCounter = new AtomicLong(0L);
	private AtomicLong expectedBackupedProducts = new AtomicLong(0L);

	// Flags to avoid conccurent export running
	private AtomicBoolean wikiExportRunning = new AtomicBoolean(false);
	private AtomicBoolean productExportRunning = new AtomicBoolean(false);
	private AtomicBoolean productCopyRunning = new AtomicBoolean(false);





	public BackupService(XWikiReadService xwikiService, ProductRepository productRepo, BackupConfig backupConfig, SerialisationService serialisationService, AggregationFacadeService aggregationService) {
		super();
		this.xwikiService = xwikiService;
		this.productRepo = productRepo;
		this.backupConfig = backupConfig;
		this.serialisationService = serialisationService;
		this.aggregationService = aggregationService;
	}

	/**
	 * This method will periodicaly export all data in a zipped file
	 */
	// TODO : Schedule from conf
	@Scheduled(initialDelay = 1000 * 3600 * 6, fixedDelay = 1000 * 3600 * 24 * 7)
	@Timed(value = "backup.products", description = "Backup of all products", extraTags = { "service" })
	public void backupProducts() {
		logger.info("Products data backup - start");

		if (!productExportRunning.compareAndSet(false, true)) {
			logger.warn("Product export is already running. Skipped");
			return;
		}

		List<ProductBackupFile> tempFiles = new ArrayList<>();
		ExecutorService executorService = null;
		AtomicBoolean producerDone = new AtomicBoolean(false);
		AtomicReference<Throwable> workerFailure = new AtomicReference<>();

		try {
			int exportThreads = Math.max(1, backupConfig.getProductsExportThreads());
			int pageSize = Math.max(1, backupConfig.getProductExportPageSize());
			LinkedBlockingQueue<Product> blockingQueue = new LinkedBlockingQueue<>(Math.max(1000, pageSize));
			expordedProductsCounter.set(0L);

			File backupFolder = ensureProductBackupFolder();
			executorService = Executors.newFixedThreadPool(exportThreads);
			List<Future<ProductBackupFile>> futures = new ArrayList<>();
			for (int i = 0; i < exportThreads; i++) {
				futures.add(executorService.submit(
						new ProductBackupThread(blockingQueue, serialisationService, i, producerDone, workerFailure)));
			}

			expectedBackupedProducts.set(productRepo.countMainIndex());

			try (Stream<Product> stream = productRepo.exportAll(pageSize)) {
				stream.forEach(product -> queueProductForBackup(blockingQueue, workerFailure, product));
			} finally {
				producerDone.set(true);
			}

			executorService.shutdown();
			if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
				executorService.shutdownNow();
				throw new IllegalStateException("Timed out waiting for product backup workers");
			}

			for (Future<ProductBackupFile> future : futures) {
				tempFiles.add(future.get());
			}

			if (workerFailure.get() != null) {
				throw new IllegalStateException("Product backup worker failed", workerFailure.get());
			}

			long exportedProducts = tempFiles.stream().mapToLong(ProductBackupFile::productCount).sum();
			publishProductBackupFiles(backupFolder.toPath(), tempFiles, expectedBackupedProducts.get(), exportedProducts,
					pageSize);
			expordedProductsCounter.set(exportedProducts);
			this.dataBackupException = null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Product backup interrupted", e);
			this.dataBackupException = e.getMessage();
			cleanupTempFiles(tempFiles);
		} catch (ExecutionException e) {
			logger.error("Error while backing up data", e.getCause());
			this.dataBackupException = Objects.toString(e.getCause().getMessage(), e.getCause().toString());
			cleanupTempFiles(tempFiles);
		} catch (Exception e) {
			logger.error("Error while backing up data", e);
			this.dataBackupException = e.getMessage();
			cleanupTempFiles(tempFiles);
		} finally {
			producerDone.set(true);
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
						executorService.shutdownNow();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					executorService.shutdownNow();
				}
			}
			productExportRunning.set(false);
		}

		logger.info("Products data backup - complete");
	}

	private File ensureProductBackupFolder() throws IOException {
		File backupFolder = new File(backupConfig.getDataBackupFolder());

		if (backupFolder.exists() && !backupFolder.isDirectory()) {
			throw new IOException("Product backup path is a file: " + backupFolder.getAbsolutePath());
		}
		if (!backupFolder.exists() && !backupFolder.mkdirs()) {
			throw new IOException("Failed to create parent directories: " + backupFolder.getAbsolutePath());
		}
		return backupFolder;
	}

	private void queueProductForBackup(LinkedBlockingQueue<Product> blockingQueue, AtomicReference<Throwable> workerFailure,
			Product product) {
		while (workerFailure.get() == null) {
			try {
				if (blockingQueue.offer(product, 1, TimeUnit.SECONDS)) {
					return;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while queueing product backup item", e);
			}
		}

		throw new IllegalStateException("Product backup worker failed", workerFailure.get());
	}

	private void publishProductBackupFiles(Path backupFolder, List<ProductBackupFile> tempFiles, long expectedProducts,
			long exportedProducts, int pageSize) throws IOException {
		List<String> fileNames = new ArrayList<>();
		for (ProductBackupFile tempFile : tempFiles) {
			Path destination = backupFolder
					.resolve(PRODUCT_BACKUP_FILE_PREFIX + tempFile.fileNumber() + PRODUCT_BACKUP_FILE_SUFFIX);
			Files.move(tempFile.path(), destination, StandardCopyOption.REPLACE_EXISTING);
			fileNames.add(destination.getFileName().toString());
		}

		ProductBackupManifest manifest = new ProductBackupManifest(Instant.now().toString(),
				System.currentTimeMillis(), expectedProducts, exportedProducts, pageSize, fileNames);
		OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(backupFolder.resolve(PRODUCT_BACKUP_MANIFEST).toFile(),
				manifest);
	}

	private void cleanupTempFiles(List<ProductBackupFile> tempFiles) {
		for (ProductBackupFile tempFile : tempFiles) {
			try {
				Files.deleteIfExists(tempFile.path());
			} catch (IOException e) {
				logger.warn("Could not delete product backup temporary file {}", tempFile.path(), e);
			}
		}
	}


	public void exportVertical(String vertical) {
	    logger.info("Exporting vertical {}", vertical);

	    try {
	        // Starting files writing threads
	        File backupFolder = new File(backupConfig.getDataBackupFolder());

	        if (backupFolder != null && backupFolder.exists() && !backupFolder.isDirectory()) {
	            throw new Exception("is a file");
	        } else if (backupFolder != null && !backupFolder.exists()) {
	            if (!backupFolder.mkdirs()) {
	                throw new IOException("Failed to create parent directories: " + backupFolder.getAbsolutePath());
	            }
	        }

	        // Creating the folders if needed
	        backupFolder.mkdirs();

	        File destFile = new File(backupFolder.getAbsolutePath() + "/" + vertical + ".gz");

	        // Try-with-resources to ensure streams are closed
	        try (FileOutputStream fos = new FileOutputStream(destFile);
	             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fos);
	             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8))) {

	            Stream<Product> str = productRepo.exportAll(vertical);

	            str.forEach(p -> {
	                try {
	                	String json = serialisationService.toJson(p);
	                    writer.write(json);
	                    writer.newLine(); // Ensure each JSON object is on a new line
	                } catch (Exception e) {
	                    logger.error("Serialization exception", e);
	                }
	            });

	        } catch (Exception e) {
	            logger.error("Error while backing up data", e);
	        }

	    } catch (Exception e) {
	        logger.error("Error while preparing for export", e);
	    }

	    logger.info("Vertical export complete : {}", vertical);
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
	        logger.error("Import file does not exist or is not a folder : {}", importFolder.getAbsolutePath());
	        return;
	    }

	    AtomicLong counter = new AtomicLong(0);
	    ExecutorService executorService = Executors.newFixedThreadPool(backupConfig.getProductImportThreads());

	    for (File importFile : importFolder.listFiles()) {
	        executorService.submit(() -> {
	            logger.info("Importing file started : {}", importFile.getAbsolutePath());
	            try (InputStream inputStream = new FileInputStream(importFile);
	                 GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
	                 InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
	                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

						List<Product> group = new ArrayList<>();
						bufferedReader.lines().forEach(line -> {
						    try {
						        group.add(translate(serialisationService.fromJson(line, Product.class)));
						        counter.incrementAndGet();
						        if (counter.get() % 1000 == 0) {
						            logger.warn("Imported items so : {}", counter.get());
						        }
						        if (group.size() == backupConfig.getImportBulkSize()) {
						        	productRepo.store(group); // Index the current group
						        	group.clear(); // Clear the group for the next batch
						        }
						    } catch (Exception e) {
						        logger.error("Error occurs in data import", e);
						    }

						});

						if (!group.isEmpty()) {
						    productRepo.store(group);
						}
						logger.info("Importing file finished : {}", importFile.getAbsolutePath());
					} catch (Exception e) {
						logger.error("Error occurs in data file processing", e);
					}
	        });
	    }

	    executorService.shutdown();
	    while (!executorService.isTerminated()) {
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Error while sleeping",e);
			}
	    }

	    logger.info("Product import : finished");
	}

	/**
	 * Copy the products index into a dedicated target index.
	 *
	 * @param suffix the suffix used to build the target index name
	 */
	public void copyTo(String suffix) {
	    String targetIndexName =   "products-" + suffix;
	    logger.info("Products copy to {} - start", targetIndexName);

	    if (productCopyRunning.get()) {
	        logger.warn("Product copy is already running. Skipped");
	        return;
	    }

	    productCopyRunning.set(true);

	    try {
	        int copyThreads = Math.max(1, backupConfig.getCopyThreads());
	        int copyBulkSize = Math.max(1, backupConfig.getCopyBulkSize());
	        int copyPageSize = Math.max(1, backupConfig.getCopyPageSize());
	        int copyQueueSize = Math.max(copyBulkSize, backupConfig.getCopyQueueSize());

	        boolean created = productRepo.createIndex(targetIndexName);
//	        if (!created) {
//	            logger.warn("Target index {} already exists. Skipping copy.", targetIndexName);
//	            productCopyRunning.set(false);
//	            return;
//	        }

	        LinkedBlockingQueue<List<Product>> blockingQueue = new LinkedBlockingQueue<>(copyQueueSize);
	        ExecutorService executorService = Executors.newFixedThreadPool(copyThreads);
	        AtomicBoolean producerDone = new AtomicBoolean(false);

	        for (int i = 0; i < copyThreads; i++) {
	            executorService.submit(() -> {
	                try {
	                    while (true) {
	                        List<Product> batch = blockingQueue.poll(1, TimeUnit.SECONDS);
	                        if (batch == null) {
	                            if (producerDone.get()) {
	                                return;
	                            }
	                            continue;
	                        }
	                        productRepo.store(batch, targetIndexName);
	                    }
	                } catch (InterruptedException e) {
	                    Thread.currentThread().interrupt();
	                    logger.error("Copy thread interrupted", e);
	                    dataCopyException = e.getMessage();
	                } catch (Exception e) {
	                    logger.error("Error while indexing products to {}", targetIndexName, e);
	                    dataCopyException = e.getMessage();
	                }
	            });
	        }

	        try (Stream<Product> stream = productRepo.exportAll(copyPageSize)) {
	            List<Product> batch = new ArrayList<>(copyBulkSize);
	            stream.forEach(product -> {
	                batch.add(product);
	                if (batch.size() >= copyBulkSize) {
	                    try {
	                        blockingQueue.put(new ArrayList<>(batch));
	                        batch.clear();
	                    } catch (InterruptedException e) {
	                        Thread.currentThread().interrupt();
	                        logger.error("Interruption while queueing copy batch", e);
	                        dataCopyException = e.getMessage();
	                    }
	                }
	            });

	            if (!batch.isEmpty()) {
	                blockingQueue.put(new ArrayList<>(batch));
	                batch.clear();
	            }
	        }

	        producerDone.set(true);
	        executorService.shutdown();
	        executorService.awaitTermination(1, TimeUnit.HOURS);
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	        logger.error("Copy operation interrupted", e);
	        dataCopyException = e.getMessage();
	    } catch (Exception e) {
	        logger.error("Error while copying products", e);
	        dataCopyException = e.getMessage();
	    } finally {
	        productCopyRunning.set(false);
	    }

	    logger.info("Products copy to {} - complete", targetIndexName);
	}

	/**
	 * Used for translation on data import
	 * @param fromJson
	 * @return
	 */
	private Product translate(Product p) {


		// Forcing fresh
//		p.setLastChange(System.currentTimeMillis());

		// Attributes migration
		// Purging aggregated
//
//		try {
//			aggregationService.sanitize(p);
//		} catch (AggregationSkipException e) {
//			logger.error("Error in import, with product sanitisation",e);
//		}


		// Setting datasources to new Map format
//		p.getMappedCategories().forEach(e -> {
//			p.getCategoriesByDatasources().put(e.getKey(), e.getValue());
//		});

		// Sanitize embeddings
		if (p.getResources() != null) {
			p.getResources().forEach(r -> {
				if (r.getImageInfo() != null && r.getImageInfo().getEmbedding() != null
						&& r.getImageInfo().getEmbedding().length > 512) {
					r.getImageInfo().setEmbedding(null);
				}
			});
		}

		return p;
	}

	/**
	 * This method will periodicaly backup the Xwiki content
	 */
	// TODO(p3,conf) : Schedule from conf
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
		File[] productBackupFiles = listProductBackupFiles(productFolder);
		long productLastModified = Arrays.stream(productBackupFiles).mapToLong(File::lastModified).max().orElse(0L);
		long productFolderSize = Arrays.stream(productBackupFiles).mapToLong(File::length).sum();


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
		if (null != dataCopyException) {
			errorMessages.put("product_copy_exception", dataCopyException);
		}

		// Check exists
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

		ProductBackupManifest manifest = readProductBackupManifest(productFolder, errorMessages);
		if (manifest != null) {
			validateProductBackupManifest(manifest, productFolder, productBackupFiles, errorMessages);
		}

		/**
		 * Checking product backup oldness
		 */
		if (oldestFileTs == Long.MAX_VALUE
				|| System.currentTimeMillis() - oldestFileTs > backupConfig.getMaxProductsBackupAgeInHours() * 3600L * 1000L) {
			errorMessages.put("product_backup_too_old", new Date (productLastModified).toString());
		}

		// Building the healthcheck

		Health health = null;

		if (errorMessages.size() == 0) {
			// All is fine
			health = Health.status(Status.UP)
					.withDetail("product_export_running", productExportRunning.get())
					.withDetail("product_backup_date", new Date (productLastModified).toString())
					.withDetail("product_backup_size", FileUtils.byteCountToDisplaySize(productFolderSize))
					.withDetail("last_product_export_items_count",  expordedProductsCounter.longValue())

					.withDetail("xwiki_export_running", wikiExportRunning.get())
					.withDetail("xwiki_backup_date", new Date (wikiLastModified).toString())
					.withDetail("xwiki_backup_size", FileUtils.byteCountToDisplaySize(wikiFileSize))
					.build();
		} else {
			health = Health.down().withDetails(errorMessages).build();
		}

		return health;
	}

	private File[] listProductBackupFiles(File productFolder) {
		File[] files = productFolder.listFiles((dir, name) -> name.startsWith(PRODUCT_BACKUP_FILE_PREFIX)
				&& name.endsWith(PRODUCT_BACKUP_FILE_SUFFIX));
		if (files == null) {
			return new File[0];
		}
		Arrays.sort(files);
		return files;
	}

	private ProductBackupManifest readProductBackupManifest(File productFolder, Map<String, String> errorMessages) {
		Path manifestPath = productFolder.toPath().resolve(PRODUCT_BACKUP_MANIFEST);
		if (!Files.exists(manifestPath)) {
			errorMessages.put("product_backup_manifest_missing", manifestPath.toString());
			return null;
		}

		try {
			return OBJECT_MAPPER.readValue(manifestPath.toFile(), ProductBackupManifest.class);
		} catch (IOException e) {
			errorMessages.put("product_backup_manifest_invalid", e.getMessage());
			return null;
		}
	}

	private void validateProductBackupManifest(ProductBackupManifest manifest, File productFolder, File[] productBackupFiles,
			Map<String, String> errorMessages) {
		if (manifest.exportedCount() < manifest.expectedCount()) {
			errorMessages.put("product_backup_manifest_exported_items_too_low",
					manifest.exportedCount() + " < " + manifest.expectedCount());
		}

		if (System.currentTimeMillis() - manifest.completedEpochMillis() > backupConfig.getMaxProductsBackupAgeInHours()
				* 3600L * 1000L) {
			errorMessages.put("product_backup_manifest_too_old", new Date(manifest.completedEpochMillis()).toString());
		}

		List<String> actualFiles = Arrays.stream(productBackupFiles).map(File::getName).toList();
		if (!actualFiles.equals(manifest.files())) {
			errorMessages.put("product_backup_manifest_file_list",
					actualFiles + " <> " + Objects.toString(manifest.files(), List.of().toString()));
		}

		if (manifest.files() != null) {
			for (String file : manifest.files()) {
				Path filePath = productFolder.toPath().resolve(file);
				if (!Files.exists(filePath)) {
					errorMessages.put("product_backup_manifest_file_missing", filePath.toString());
				}
			}
		}
	}

	/**
	 * Metadata written after a successful complete product backup publication.
	 *
	 * @param completedAt completion timestamp in ISO-8601 format
	 * @param completedEpochMillis completion timestamp in epoch milliseconds
	 * @param expectedCount expected product count before streaming
	 * @param exportedCount successfully serialized product count
	 * @param pageSize Elasticsearch export page size
	 * @param files final backup files published by the export
	 */
	public record ProductBackupManifest(String completedAt, long completedEpochMillis, long expectedCount,
			long exportedCount, int pageSize, List<String> files) {
	}

}

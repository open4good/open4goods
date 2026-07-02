package org.open4goods.api.services.backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.open4goods.model.product.Product;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread is in charge of serialisation and compression of products in a given file, polling from a BlockingQueue
 */
public class ProductBackupThread implements Callable<ProductBackupThread.ProductBackupFile> {

	private static final Logger logger = LoggerFactory.getLogger(ProductBackupThread.class);

    private final LinkedBlockingQueue<Product> queue;
    private final SerialisationService serialisationService;
    private final int fileNumber;
    private final AtomicBoolean producerDone;
    private final AtomicReference<Throwable> failure;
    
    private BufferedWriter writer;
    private File tmpFile;
    private long writtenProducts;

    public ProductBackupThread(LinkedBlockingQueue<Product> queue, SerialisationService serialisationService, int fileNumber,
            AtomicBoolean producerDone, AtomicReference<Throwable> failure) throws Exception {
        this.queue = queue;
        this.serialisationService = serialisationService;
        this.fileNumber = fileNumber;
        this.producerDone = producerDone;
        this.failure = failure;

        logger.info("Starting product backup thread {}", fileNumber);
        try {

            // Create temporary file for backup
            tmpFile = File.createTempFile("product-backup-" + fileNumber, ".gz");
            
            // Streams and writers
            FileOutputStream fos = new FileOutputStream(tmpFile);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fos);
            writer = new BufferedWriter(new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            logger.error("Failed to initialize file resources", e);
            throw e;
        }
    }

    @Override
    public ProductBackupFile call() {
        try {
            processQueue();
            return finalizeBackup();
        } catch (RuntimeException e) {
            failure.compareAndSet(null, e);
            if (tmpFile != null) {
                tmpFile.delete();
            }
            throw e;
        }
    }

    /**
     * Reads from the queue, serializes and compresses into a temporary file until the producer is done.
     */
    private void processQueue() {
        logger.info("Starting product consuming for thread {}", fileNumber);
        while (failure.get() == null) {
            try {
                Product product = queue.poll(1, TimeUnit.SECONDS);
                
                if (product == null) {
                    if (producerDone.get() && queue.isEmpty()) {
                        logger.info("Handling done for product backup thread {}", fileNumber);
                        break;
                    }
                    continue;
                }

                String json = serialisationService.toJson(product);
                writer.write(json);
                writer.newLine();
                writtenProducts++;
            } catch (InterruptedException e) {
                logger.warn("Backup thread interrupted", e);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Product backup thread interrupted", e);
            } catch (Exception e) {
                logger.error("Serialization exception", e);
                throw new IllegalStateException("Product backup serialization failed", e);
            } 
        }
    }

    /**
     * Releases resources and returns the completed temporary file.
     */
    private ProductBackupFile finalizeBackup()  {
        IOUtils.closeQuietly(writer);

        if (!tmpFile.exists()) {
            throw new IllegalStateException("Backup temporary file does not exist: " + tmpFile.getAbsolutePath());
        }

        logger.info("Backup temporary file successfully created: {}", tmpFile.getAbsolutePath());
        return new ProductBackupFile(fileNumber, tmpFile.toPath(), writtenProducts);
    }

    /**
     * Temporary backup file produced by a worker.
     *
     * @param fileNumber the final backup file number
     * @param path the temporary file path
     * @param productCount number of products written by this worker
     */
    public record ProductBackupFile(int fileNumber, Path path, long productCount) {
    }

}

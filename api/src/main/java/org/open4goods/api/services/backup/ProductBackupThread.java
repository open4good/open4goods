package org.open4goods.api.services.backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.open4goods.model.product.Product;
import org.open4goods.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread is in charge of serialisation and compression of products in a given file, polling from a BlockingQueue
 */
public class ProductBackupThread implements Runnable {

    private static final int MINIMUM_TMP_FILE_SIZE = 1024 * 1024 * 100;

	private static final Logger logger = LoggerFactory.getLogger(ProductBackupThread.class);

    private final File outputFolder;
    private final LinkedBlockingQueue<Product> queue;
    private final SerialisationService serialisationService;
    private final int fileNumber;
    
    private BufferedWriter writer;
    private File tmpFile;

    public ProductBackupThread(File outputFolder, LinkedBlockingQueue<Product> queue, SerialisationService serialisationService, int fileNumber) throws Exception {
        this.outputFolder = outputFolder;
        this.queue = queue;
        this.serialisationService = serialisationService;
        this.fileNumber = fileNumber;

        logger.info("Starting product backuping thread {}", fileNumber);
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
    public void run() {
				// Data writing
				processQueue();
				// File closing, and on 
				finalizeBackup();
    }

    /**
     * Readinf from the queue, serialize and compress into 
     * the target file, until no elements availlable
     */
    private void processQueue() {
        logger.info("Starting product consuming for thread {}", fileNumber);
        while (true) {
            try {
                Product product = queue.poll(5, TimeUnit.SECONDS);
                
                if (product == null) {
                	logger.info("Handling done for this thread");
                	break;
                } else {
                    String json = serialisationService.toJson(product);
                    try {
						writer.write(json);
						writer.newLine(); // Ensure each JSON object is on a new line
					} catch (IOException e) {
						   logger.error("Serialiation exception", e);
					}
                }
            } catch (InterruptedException e) {
                logger.warn("Backup thread interrupted", e);
                Thread.currentThread().interrupt(); // Restore interrupt status
            } 
        }
    }

    /**
     * Resources releasing, and moving file to final destination
     */
    private void finalizeBackup()  {
        IOUtils.closeQuietly(writer);

        if (!tmpFile.exists()) {
            logger.error("Backup file does not exist: " + tmpFile.getAbsolutePath());
        } else if (tmpFile.length() < MINIMUM_TMP_FILE_SIZE) {
            logger.error("Backup file size is too low");
            tmpFile.delete();
        }  else {
            try {
            	Path dest = Path.of(outputFolder.toPath() +"/products-backup-"+fileNumber+".gz");
                Files.move(tmpFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Backup file successfully created: " + outputFolder.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to move backup file to final destination", e);
                throw new UncheckedIOException(e);
            }
        }
    }


}

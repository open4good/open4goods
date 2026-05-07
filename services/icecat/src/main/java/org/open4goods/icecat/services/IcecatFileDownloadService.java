package org.open4goods.icecat.services;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Handles downloading and decompressing Icecat bulk XML export files.
 *
 * <p>Caches files locally: if the target file already exists it is returned without
 * re-downloading (no refresh policy yet). Credentials are read from {@link IcecatConfiguration}.
 *
 * <p>Extracted from the duplicate {@code getCachedFile()} methods that existed in
 * {@code FeatureLoader}, {@code CategoryLoader}, and {@code IcecatService}.
 */
public class IcecatFileDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatFileDownloadService.class);

    private final RemoteFileCachingService fileCachingService;
    private final String remoteCachingFolder;
    private final IcecatConfiguration iceCatConfig;

    public IcecatFileDownloadService(
            RemoteFileCachingService fileCachingService,
            String remoteCachingFolder,
            IcecatConfiguration iceCatConfig) {
        this.fileCachingService = fileCachingService;
        this.remoteCachingFolder = remoteCachingFolder;
        this.iceCatConfig = iceCatConfig;
    }

    /**
     * Returns a local {@link File} for the given Icecat export URL, downloading and
     * decompressing it if not already cached.
     *
     * @param url the Icecat bulk export URL (gzip-compressed XML)
     * @return the local decompressed file
     * @throws TechnicalException if the download or decompression fails
     */
    public File getOrDownload(String url) throws TechnicalException {
        LOGGER.info("Retrieving file : {}", url);
        File destFile = new File(remoteCachingFolder + File.separator + IdHelper.getHashedName(url));
        if (destFile.exists()) {
            LOGGER.info("File {} already cached", url);
            return destFile;
        }
        File tmpFile = new File(remoteCachingFolder + File.separator + "tmp-" + IdHelper.getHashedName(url));
        try {
            LOGGER.info("Starting download : {}", url);
            fileCachingService.downloadTo(iceCatConfig.getUser(), iceCatConfig.getPassword(), url, tmpFile);
            LOGGER.info("Uncompressing file : {}", tmpFile);
            fileCachingService.decompressGzipFile(tmpFile, destFile);
            LOGGER.info("File {} uncompressed", url);
            return destFile;
        } catch (Exception e) {
            throw new TechnicalException("Error retrieving resource " + url, e);
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    /**
     * Returns the local path that would be used for the given URL (whether or not it exists).
     * Useful for constructing derived file names (e.g. minified variants).
     *
     * @param url the remote URL
     * @return the local file (may not exist yet)
     */
    public File localFileFor(String url) {
        return new File(remoteCachingFolder + File.separator + IdHelper.getHashedName(url));
    }

    public String getRemoteCachingFolder() {
        return remoteCachingFolder;
    }
}

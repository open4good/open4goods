package org.open4goods.services.geocode.service.maxmind;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.services.geocode.config.yml.MaxMindProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provides the locally cached MaxMind GeoIP database path.
 */
@Component
public class MaxMindDatasetProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxMindDatasetProvider.class);

    private final RemoteFileCachingService remoteFileCachingService;
    private final MaxMindProperties maxMindProperties;

    /**
     * Creates a new MaxMind dataset provider.
     *
     * @param remoteFileCachingService caching service
     * @param maxMindProperties MaxMind configuration
     */
    public MaxMindDatasetProvider(@org.springframework.beans.factory.annotation.Qualifier("geocodeRemoteFileCachingService") RemoteFileCachingService remoteFileCachingService,
            MaxMindProperties maxMindProperties)
    {
        this.remoteFileCachingService = remoteFileCachingService;
        this.maxMindProperties = maxMindProperties;
    }

    /**
     * Returns the local path to the cached GeoIP database.
     *
     * @return database path
     */
    public Path getDatabasePath()
    {
        try
        {
            File archiveFile = remoteFileCachingService.getResource(
                    maxMindProperties.getUrl(),
                    maxMindProperties.getRefreshInDays());
            Path archivePath = archiveFile.toPath();
            Path extractedPath = archivePath.getParent().resolve(maxMindProperties.getDatabaseFileName());

            if (isExtractionUpToDate(archivePath, extractedPath))
            {
                return extractedPath;
            }

            LOGGER.info("Extracting MaxMind database from {} to {}", archivePath, extractedPath);
            extractDatabase(archivePath, extractedPath, maxMindProperties.getDatabaseFileName());
            Files.setLastModifiedTime(extractedPath, Files.getLastModifiedTime(archivePath));
            return extractedPath;
        }
        catch (IOException | InvalidParameterException ex)
        {
            throw new IllegalStateException("Failed to prepare MaxMind dataset", ex);
        }
    }

    /**
     * Checks if the extracted database is newer than the archive.
     *
     * @param archivePath archive path
     * @param extractedPath extracted database path
     * @return true when extraction is current
     * @throws IOException when file metadata cannot be read
     */
    private boolean isExtractionUpToDate(Path archivePath, Path extractedPath) throws IOException
    {
        if (Files.exists(extractedPath))
        {
            return Files.getLastModifiedTime(extractedPath)
                    .compareTo(Files.getLastModifiedTime(archivePath)) >= 0;
        }
        return false;
    }

    /**
     * Extracts the expected database file from the tar.gz archive.
     *
     * @param archivePath archive path
     * @param extractedPath destination path
     * @param expectedFileName expected database filename
     * @throws IOException when extraction fails
     */
    private void extractDatabase(Path archivePath, Path extractedPath, String expectedFileName) throws IOException
    {
        boolean extracted = false;
        try (InputStream in = Files.newInputStream(archivePath);
                GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn))
        {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null)
            {
                if (entry.isDirectory())
                {
                    continue;
                }
                String entryName = entry.getName();
                if (entryName.endsWith(expectedFileName))
                {
                    Files.copy(tarIn, extractedPath, StandardCopyOption.REPLACE_EXISTING);
                    extracted = true;
                    break;
                }
            }
        }
        if (!extracted)
        {
            throw new IOException("Expected MaxMind database not found in archive: " + expectedFileName);
        }
    }
}

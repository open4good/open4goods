package org.open4goods.services.geocode.service.geonames;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.services.geocode.config.yml.GeoNamesProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provides the locally cached GeoNames dataset path.
 */
@Component
public class GeoNamesDatasetProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesDatasetProvider.class);

    private final RemoteFileCachingService remoteFileCachingService;
    private final GeoNamesProperties geoNamesProperties;

    /**
     * Creates a new dataset provider.
     *
     * @param remoteFileCachingService caching service
     * @param geoNamesProperties GeoNames configuration
     */
    public GeoNamesDatasetProvider(RemoteFileCachingService remoteFileCachingService,
            GeoNamesProperties geoNamesProperties)
    {
        this.remoteFileCachingService = remoteFileCachingService;
        this.geoNamesProperties = geoNamesProperties;
    }

    /**
     * Returns the local path to the cached cities5000.txt dataset.
     *
     * @return dataset path
     */
    public Path getDatasetPath()
    {
        try
        {
            File zipFile = remoteFileCachingService.getResource(
                    geoNamesProperties.getUrl(),
                    geoNamesProperties.getRefreshInDays());
            Path zipPath = zipFile.toPath();
            Path extractedPath = zipPath.getParent().resolve(geoNamesProperties.getExtractedFileName());

            if (isExtractionUpToDate(zipPath, extractedPath))
            {
                return extractedPath;
            }

            LOGGER.info("Extracting GeoNames dataset from {} to {}", zipPath, extractedPath);
            extractFile(zipPath, extractedPath, geoNamesProperties.getExtractedFileName());
            Files.setLastModifiedTime(extractedPath, Files.getLastModifiedTime(zipPath));
            return extractedPath;
        }
        catch (IOException | InvalidParameterException ex)
        {
            throw new IllegalStateException("Failed to prepare GeoNames dataset", ex);
        }
    }

    private boolean isExtractionUpToDate(Path zipPath, Path extractedPath) throws IOException
    {
        if (Files.exists(extractedPath))
        {
            // Avoid re-extracting when the cached text file is newer than the zip.
            return Files.getLastModifiedTime(extractedPath)
                    .compareTo(Files.getLastModifiedTime(zipPath)) >= 0;
        }
        return false;
    }

    private void extractFile(Path zipPath, Path extractedPath, String expectedFileName) throws IOException
    {
        boolean extracted = false;
        try (InputStream in = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(in))
        {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                String entryName = entry.getName();
                if (entryName.endsWith(expectedFileName))
                {
                    Files.copy(zis, extractedPath, StandardCopyOption.REPLACE_EXISTING);
                    extracted = true;
                    break;
                }
            }
        }
        if (!extracted)
        {
            throw new IOException("Expected GeoNames entry not found in archive: " + expectedFileName);
        }
    }
}

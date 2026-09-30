package org.open4goods.icecat.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatCatalogueCategory;
import org.open4goods.icecat.model.IcecatCatalogueInventory;
import org.open4goods.icecat.services.loader.IcecatReferenceCatalogueReader;
import org.open4goods.model.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an {@link IcecatCatalogueInventory} by streaming the Icecat reference-catalogue export
 * files, giving {@link IcecatMappingCoverageService} coverage denominators that do not depend on
 * a prior Elasticsearch sync.
 *
 * <p>Each configured file URI ({@code icecat-feature-config.*-file-uri}) may point to a local
 * file already on disk (used as-is, gzip-decompressed if its name ends in {@code .gz}, no
 * network or credential involved) or to a remote URL, in which case download is delegated to
 * {@link IcecatFileDownloadService} using the configured Open Icecat account credentials.
 */
public class IcecatCatalogueInventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatCatalogueInventoryService.class);

    private final IcecatConfiguration iceCatConfig;
    private final IcecatFileDownloadService fileDownloadService;

    public IcecatCatalogueInventoryService(IcecatConfiguration iceCatConfig, IcecatFileDownloadService fileDownloadService) {
        this.iceCatConfig = Objects.requireNonNull(iceCatConfig, "iceCatConfig must not be null");
        this.fileDownloadService = Objects.requireNonNull(fileDownloadService, "fileDownloadService must not be null");
    }

    /**
     * Streams CategoriesList, FeaturesList, FeatureGroupsList and LanguageList to build the
     * catalogue-wide coverage denominators.
     *
     * @throws TechnicalException if a configured file is missing, unreachable, or malformed
     */
    public IcecatCatalogueInventory buildInventory() throws TechnicalException {
        List<IcecatCatalogueCategory> categories = readCategories(
                iceCatConfig.getCategoriesListFileUri(), "CategoriesList");
        int featureCount = readCount(iceCatConfig.getFeaturesListFileUri(), IcecatReferenceCatalogueReader::countFeatures);
        int featureGroupCount = readCount(
                iceCatConfig.getFeatureGroupsFileUri(), IcecatReferenceCatalogueReader::countFeatureGroups);
        int languageCount = readCount(iceCatConfig.getLanguageListFileUri(), IcecatReferenceCatalogueReader::countLanguages);
        return new IcecatCatalogueInventory(featureCount, featureGroupCount, languageCount, categories);
    }

    private List<IcecatCatalogueCategory> readCategories(String uriOrPath, String listElementName) throws TechnicalException {
        try (InputStream in = open(uriOrPath)) {
            return IcecatReferenceCatalogueReader.readCategories(in, listElementName);
        } catch (Exception e) {
            throw new TechnicalException("Error streaming Icecat categories from " + describe(uriOrPath), e);
        }
    }

    private int readCount(String uriOrPath, StreamCounter counter) throws TechnicalException {
        try (InputStream in = open(uriOrPath)) {
            return counter.count(in);
        } catch (Exception e) {
            throw new TechnicalException("Error streaming Icecat reference file " + describe(uriOrPath), e);
        }
    }

    private InputStream open(String uriOrPath) throws TechnicalException {
        if (uriOrPath == null || uriOrPath.isBlank()) {
            throw new TechnicalException("No Icecat reference file uri configured");
        }
        File local = new File(uriOrPath);
        if (local.isFile()) {
            LOGGER.info("Streaming Icecat reference file from local path {}", local);
            return openMaybeGzip(local);
        }
        LOGGER.info("Streaming Icecat reference file from configured URL");
        File downloaded = fileDownloadService.getOrDownload(uriOrPath);
        return openMaybeGzip(downloaded);
    }

    private InputStream openMaybeGzip(File file) throws TechnicalException {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            return file.getName().endsWith(".gz") ? new GZIPInputStream(in) : in;
        } catch (Exception e) {
            throw new TechnicalException("Error opening Icecat reference file " + file, e);
        }
    }

    private String describe(String uriOrPath) {
        return uriOrPath == null ? "<unconfigured>" : uriOrPath;
    }

    @FunctionalInterface
    private interface StreamCounter {
        int count(InputStream in) throws Exception;
    }
}

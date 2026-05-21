package org.open4goods.icecat.services.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.brand.model.Brand;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureGroup;
import org.open4goods.icecat.model.IcecatModel;
import org.open4goods.icecat.model.IcecatSupplier;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Loads Icecat reference data (features, feature groups, brands/suppliers) from
 * bulk XML export files into structures used by {@link org.open4goods.icecat.services.IcecatService}.
 *
 * <p>File download and caching is delegated to {@link IcecatFileDownloadService}.
 * Elasticsearch persistence is handled by {@link org.open4goods.icecat.services.IcecatIndexService}.
 */
public class FeatureLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureLoader.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration iceCatConfig;
    private final IcecatFileDownloadService fileDownloadService;
    private final BrandService brandService;

    private final Map<Integer, IcecatFeature> featuresById = new HashMap<>();
    private final Map<Integer, IcecatFeatureGroup> featureGroupsById = new HashMap<>();
    private final List<IcecatSupplier> icecatSuppliers = new ArrayList<>();

    public FeatureLoader(
            XmlMapper xmlMapper,
            IcecatConfiguration iceCatConfig,
            IcecatFileDownloadService fileDownloadService,
            BrandService brandService) {
        this.xmlMapper = xmlMapper;
        this.iceCatConfig = iceCatConfig;
        this.fileDownloadService = fileDownloadService;
        this.brandService = brandService;
    }

    /**
     * Loads all features from FeaturesList.xml into {@link #featuresById}.
     *
     * @throws TechnicalException if the file cannot be downloaded or parsed
     */
    public void loadFeatures() throws TechnicalException {
        if (iceCatConfig.getFeaturesListFileUri() == null) {
            LOGGER.error("No features list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getFeaturesListFileUri());
        File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getFeaturesListFileUri());
        try {
            List<IcecatFeature> features = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getFeaturesList().getFeatures();
            features.forEach(feature -> {
                Integer id = feature.getId();
                featuresById.put(id, feature);
            });
        } catch (Exception e) {
            LOGGER.error("Error while loading features", e);
        }
        LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
    }

    /**
     * Loads all feature groups from FeatureGroupsList.xml into {@link #featureGroupsById}.
     *
     * @throws TechnicalException if the file cannot be downloaded or parsed
     */
    public void loadFeatureGroups() throws TechnicalException {
        if (iceCatConfig.getFeatureGroupsFileUri() == null) {
            LOGGER.error("No features group list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getFeatureGroupsFileUri());
        File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getFeatureGroupsFileUri());
        try {
            List<IcecatFeatureGroup> groups = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getFeatureGroupsList().getFeatureGroups();
            for (IcecatFeatureGroup fg : groups) {
                featureGroupsById.put(fg.getId(), fg);
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading feature groups", e);
        }
        LOGGER.info("End loading of feature groups from {}", iceCatConfig.getFeatureGroupsFileUri());
    }

    /**
     * Loads all brands/suppliers from SuppliersList.xml, resolves them via {@link BrandService},
     * and stores the raw supplier objects in {@link #icecatSuppliers} for ES indexing.
     *
     * @throws TechnicalException if the file cannot be downloaded or parsed
     */
    public void loadBrands() throws TechnicalException {
        if (iceCatConfig.getBrandsListFileUri() == null) {
            LOGGER.error("No brands list file uri configured");
            return;
        }
        LOGGER.info("Getting brands file from {}", iceCatConfig.getBrandsListFileUri());
        File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getBrandsListFileUri());
        try {
            List<IcecatSupplier> suppliers = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getSuppliersList().getSuppliers();
            for (IcecatSupplier supplier : suppliers) {
                icecatSuppliers.add(supplier);
                Brand brand = brandService.resolve(supplier.getEffectiveName());
                if (brand == null) {
                    brand = new Brand();
                    brand.setBrandName(IdHelper.brandName(supplier.getEffectiveName()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading brands", e);
        }
        LOGGER.info("End loading of brands from {}", iceCatConfig.getBrandsListFileUri());
    }

    public Map<Integer, IcecatFeature> getFeaturesById() {
        return featuresById;
    }

    public Map<Integer, IcecatFeatureGroup> getFeatureGroupsById() {
        return featureGroupsById;
    }

    /** Returns all suppliers loaded from SuppliersList.xml, available for ES indexing. */
    public List<IcecatSupplier> getIcecatSuppliers() {
        return icecatSuppliers;
    }
}

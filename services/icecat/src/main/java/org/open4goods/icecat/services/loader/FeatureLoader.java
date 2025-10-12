package org.open4goods.icecat.services.loader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.open4goods.commons.model.data.Brand;
import org.open4goods.commons.services.BrandService;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureGroup;
import org.open4goods.icecat.model.IcecatModel;
import org.open4goods.icecat.model.IcecatSupplier;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class FeatureLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureLoader.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration iceCatConfig;
    private final RemoteFileCachingService fileCachingService;
    private final String remoteCachingFolder;
    private final BrandService brandService;

    private final Map<Integer, IcecatFeature> featuresById = new HashMap<>();
    private final Map<String, Set<Integer>> featuresByNames = new HashMap<>();
    private final Map<Integer, IcecatFeatureGroup> featureGroupsById = new HashMap<>();

    public FeatureLoader(XmlMapper xmlMapper,
                         IcecatConfiguration iceCatConfig,
                         RemoteFileCachingService fileCachingService,
                         String remoteCachingFolder,
                         BrandService brandService) {
        this.xmlMapper = xmlMapper;
        this.iceCatConfig = iceCatConfig;
        this.fileCachingService = fileCachingService;
        this.remoteCachingFolder = remoteCachingFolder;
        this.brandService = brandService;
    }

    public void loadFeatures() throws TechnicalException {
        if (iceCatConfig.getFeaturesListFileUri() == null) {
            LOGGER.error("No features list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getFeaturesListFileUri());
        File icecatFile = getCachedFile(iceCatConfig.getFeaturesListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
        try {
            List<IcecatFeature> features = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getFeaturesList().getFeatures();
            features.forEach(feature -> {
                Integer id = Integer.valueOf(feature.getID());
                featuresById.put(id, feature);
                feature.getNames().getNames().forEach(name -> {
                    String val = IdHelper.normalizeAttributeName(name.getTextValue());
                    Set<Integer> fIds = featuresByNames.computeIfAbsent(val, k -> new HashSet<>());
                    fIds.add(id);
                });
            });
        } catch (Exception e) {
            LOGGER.error("Error while loading features", e);
        }
        LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
    }

    public void loadFeatureGroups() throws TechnicalException {
        if (iceCatConfig.getFeatureGroupsFileUri() == null) {
            LOGGER.error("No features group list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getFeatureGroupsFileUri());
        File icecatFile = getCachedFile(iceCatConfig.getFeatureGroupsFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
        try {
            List<IcecatFeatureGroup> groups = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getFeatureGroupsList().getFeatureGroups();
            for (IcecatFeatureGroup fg : groups) {
                featureGroupsById.put(fg.getID(), fg);
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading feature groups", e);
        }
        LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
    }

    public void loadBrands() throws TechnicalException {
        if (iceCatConfig.getBrandsListFileUri() == null) {
            LOGGER.error("No brands list file uri configured");
            return;
        }
        LOGGER.info("Getting brands file from {}", iceCatConfig.getBrandsListFileUri());
        File icecatFile = getCachedFile(iceCatConfig.getBrandsListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
        try {
            List<IcecatSupplier> suppliers = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getSuppliersList().getSuppliers();
            for (IcecatSupplier supplier : suppliers) {
                Brand brand = brandService.resolve(supplier.getName());
                if (brand == null) {
                    brand = new Brand();
                    brand.setBrandName(IdHelper.brandName(supplier.getName()));
                }
                // TODO handle brand logos
                // brandService.saveBrand(brand);
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading features", e);
        }
        LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
    }

    private File getCachedFile(String url, String user, String password) throws TechnicalException {
        LOGGER.info("Retrieving file : {}", url);
        File destFile = new File(remoteCachingFolder + File.separator + IdHelper.getHashedName(url));
        if (destFile.exists()) {
            LOGGER.info("File {} already cached", url);
            return destFile;
        }
        File tmpFile = new File(remoteCachingFolder + File.separator + "tmp-" + IdHelper.getHashedName(url));
        try {
            LOGGER.info("Starting download : {}", url);
            fileCachingService.downloadTo(user, password, url, tmpFile);
            LOGGER.info("Uncompressing file : {}", tmpFile);
            fileCachingService.decompressGzipFile(tmpFile, destFile);
            LOGGER.info("File {} uncompressed", url);
            return destFile;
        } catch (Exception e) {
            throw new TechnicalException("Error retrieving resource", e);
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    public Map<Integer, IcecatFeature> getFeaturesById() {
        return featuresById;
    }

    public Map<String, Set<Integer>> getFeaturesByNames() {
        return featuresByNames;
    }

    public Map<Integer, IcecatFeatureGroup> getFeatureGroupsById() {
        return featureGroupsById;
    }
}

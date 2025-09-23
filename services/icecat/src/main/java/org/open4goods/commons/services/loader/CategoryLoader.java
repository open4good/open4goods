package org.open4goods.commons.services.loader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.commons.config.yml.IcecatConfiguration;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.icecat.IcecatCategory;
import org.open4goods.model.icecat.IcecatCategoryFeatureGroup;
import org.open4goods.model.icecat.IcecatFeature;
import org.open4goods.model.icecat.IcecatFeatureGroup;
import org.open4goods.model.icecat.IcecatModel;
import org.open4goods.model.icecat.IcecatName;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class CategoryLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryLoader.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration iceCatConfig;
    private final RemoteFileCachingService fileCachingService;
    private final String remoteCachingFolder;
    private final VerticalsConfigService verticalsConfigService;
    private final FeatureLoader featureLoader;

    private final Map<Integer, IcecatCategory> categoriesById = new HashMap<>();

    public CategoryLoader(XmlMapper xmlMapper,
                          IcecatConfiguration iceCatConfig,
                          RemoteFileCachingService fileCachingService,
                          String remoteCachingFolder,
                          VerticalsConfigService verticalsConfigService,
                          FeatureLoader featureLoader) {
        this.xmlMapper = xmlMapper;
        this.iceCatConfig = iceCatConfig;
        this.fileCachingService = fileCachingService;
        this.remoteCachingFolder = remoteCachingFolder;
        this.verticalsConfigService = verticalsConfigService;
        this.featureLoader = featureLoader;
    }

    public void loadCategories() throws TechnicalException {
        if (iceCatConfig.getCategoriesListFileUri() == null) {
            LOGGER.error("No categories list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getCategoriesListFileUri());
        File icecatFile = getCachedFile(iceCatConfig.getCategoriesListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
        try {
            List<IcecatCategory> categories = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getCategoryList().getCategories();
            categories.forEach(category -> categoriesById.put(category.getID(), category));
        } catch (Exception e) {
            LOGGER.error("Error while loading categories", e);
        }
        LOGGER.info("End loading of categories from {}", iceCatConfig.getCategoriesListFileUri());
    }

    public void loadCategoryFeatureList() throws TechnicalException {
        if (iceCatConfig.getCategoryFeatureListFileUri() == null) {
            LOGGER.error("No category features list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getCategoryFeatureListFileUri());
        File icecatMimified = new File(remoteCachingFolder + File.separator + IdHelper.getHashedName(iceCatConfig.getCategoryFeatureListFileUri() + ".min"));
        if (!icecatMimified.exists()) {
            LOGGER.info("Minified file not found, generating mimified version");
            File icecatFile = new File(remoteCachingFolder + File.separator + IdHelper.getHashedName(iceCatConfig.getCategoryFeatureListFileUri()));
            icecatFile = getCachedFile(iceCatConfig.getCategoryFeatureListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
            LOGGER.info("Start generating mimified version");
            icecatMimified.delete();
            AtomicBoolean inMeasure = new AtomicBoolean(false);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(icecatMimified, true))) {
                Files.lines(icecatFile.toPath()).forEach(l -> {
                    try {
                        if (l.contains("<Measure ")) {
                            inMeasure.set(true);
                        }
                        if (!inMeasure.get()) {
                            if (!l.contains("<Name") && !l.contains("<RestrictedValue")) {
                                writer.write(l);
                                writer.newLine();
                            }
                        }
                        if (l.contains("</Measure")) {
                            inMeasure.set(false);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error writing line", e);
                    }
                });
                LOGGER.info("End generating mimified version : {}", icecatMimified.getAbsolutePath());
                LOGGER.info("Cleaning up the uncompressed file");
                IOUtils.closeQuietly(writer);
            } catch (IOException e) {
                LOGGER.error("Error writing file", e);
            }
        }
        try {
            LOGGER.info("DOM Parsing of {}", icecatMimified);
            List<IcecatCategory> categories = xmlMapper.readValue(icecatMimified, IcecatModel.class)
                    .getResponse().getCategoryFeaturesList().getCategories();
            for (IcecatCategory category : categories) {
                int catId = category.getID();
                VerticalConfig vertical = verticalsConfigService.getByIcecatCategoryId(catId);
                if (vertical != null) {
                    updateVertical(category, vertical);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading category features list", e);
        }
        LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
    }

    private void updateVertical(IcecatCategory category, VerticalConfig vertical) {
        Map<Integer, FeatureGroup> featureGroupById = new HashMap<>();
        if (category.getCategoryFeatureGroups() != null) {
            for (IcecatCategoryFeatureGroup cfg : category.getCategoryFeatureGroups()) {
                int cfgId = cfg.getID();
                for (IcecatFeatureGroup ifg : cfg.getFeatureGroups()) {
                    FeatureGroup fg = vertical.getOrCreateByIceCatCategoryFeatureGroup(ifg.getID());
                    List<IcecatName> names = featureLoader.getFeatureGroupsById().get(ifg.getID()).getNames();
                    IcecatName defName = names.stream().filter(e -> e.getLangId() == 1).findFirst().orElse(null);
                    if (defName != null) {
                        fg.getName().put("default", defName.getValue());
                    }
                    IcecatName frName = names.stream().filter(e -> e.getLangId() == 3).findFirst().orElse(null);
                    if (frName != null) {
                        fg.getName().put("fr", frName.getValue());
                    }
                    FeatureGroup tmpid = featureGroupById.get(cfgId);
                    if (tmpid != null && tmpid.getIcecatCategoryFeatureGroupId() != fg.getIcecatCategoryFeatureGroupId()) {
                        LOGGER.warn("Feature group {} already present in category feature group {}", ifg.getID(), cfgId);
                    } else {
                        featureGroupById.put(cfgId, fg);
                    }
                }
            }
        }
        if (category.getFeatures() != null) {
            for (IcecatFeature feature : category.getFeatures()) {
                int categoryFeatureGroupId = feature.getCategoryFeatureGroup_ID();
                FeatureGroup fg = featureGroupById.get(categoryFeatureGroupId);
                if (fg != null) {
                    Integer fId = Integer.valueOf(feature.getID());
                    if (!fg.getFeaturesId().contains(fId)) {
                        fg.getFeaturesId().add(fId);
                    } else {
                        LOGGER.warn("Feature {} already present in feature group {}", fId, fg);
                    }
                } else {
                    LOGGER.warn("No feature group found for feature {}", feature);
                }
            }
        }
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

    public Map<Integer, IcecatCategory> getCategoriesById() {
        return categoriesById;
    }
}

package org.open4goods.icecat.services.loader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatCategory;
import org.open4goods.icecat.model.IcecatCategoryFeatureGroup;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureGroup;
import org.open4goods.icecat.model.IcecatModel;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Loads Icecat product categories and category-feature mappings from bulk XML export files.
 *
 * <p>After loading, this service updates matching {@link VerticalConfig} instances via
 * {@link VerticalsConfigService} so that each vertical knows which Icecat feature groups
 * and features apply to it.
 *
 * <p>File download and caching is delegated to {@link IcecatFileDownloadService}.
 */
public class CategoryLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryLoader.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration iceCatConfig;
    private final IcecatFileDownloadService fileDownloadService;
    private final VerticalsConfigService verticalsConfigService;
    private final FeatureLoader featureLoader;

    private final Map<Integer, IcecatCategory> categoriesById = new HashMap<>();

    public CategoryLoader(
            XmlMapper xmlMapper,
            IcecatConfiguration iceCatConfig,
            IcecatFileDownloadService fileDownloadService,
            VerticalsConfigService verticalsConfigService,
            FeatureLoader featureLoader) {
        this.xmlMapper = xmlMapper;
        this.iceCatConfig = iceCatConfig;
        this.fileDownloadService = fileDownloadService;
        this.verticalsConfigService = verticalsConfigService;
        this.featureLoader = featureLoader;
    }

    /**
     * Loads all categories from CategoriesList.xml into {@link #categoriesById}.
     *
     * @throws TechnicalException if the file cannot be downloaded or parsed
     */
    public void loadCategories() throws TechnicalException {
        if (iceCatConfig.getCategoriesListFileUri() == null) {
            LOGGER.error("No categories list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getCategoriesListFileUri());
        File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getCategoriesListFileUri());
        try {
            List<IcecatCategory> categories = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getCategoryList().getCategories();
            categories.forEach(category -> categoriesById.put(category.getId(), category));
        } catch (Exception e) {
            LOGGER.error("Error while loading categories", e);
        }
        LOGGER.info("End loading of categories from {}", iceCatConfig.getCategoriesListFileUri());
    }

    /**
     * Loads the CategoryFeaturesList.xml, which maps features to categories.
     * For each category that matches a configured vertical, updates the vertical's
     * feature group definitions.
     *
     * <p>The file is minified before DOM-parsing to remove large {@code <Name>} and
     * {@code <RestrictedValue>} elements that are not needed and would bloat the DOM.
     *
     * @throws TechnicalException if the file cannot be downloaded or parsed
     */
    public void loadCategoryFeatureList() throws TechnicalException {
        if (iceCatConfig.getCategoryFeatureListFileUri() == null) {
            LOGGER.error("No category features list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getCategoryFeatureListFileUri());
        File icecatMimified = new File(
                fileDownloadService.getRemoteCachingFolder() + File.separator
                + IdHelper.getHashedName(iceCatConfig.getCategoryFeatureListFileUri() + ".min"));

        if (!icecatMimified.exists()) {
            LOGGER.info("Minified file not found, generating minified version");
            File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getCategoryFeatureListFileUri());
            LOGGER.info("Start generating minified version");
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
                LOGGER.info("End generating minified version : {}", icecatMimified.getAbsolutePath());
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
                int catId = category.getId();
                VerticalConfig vertical = verticalsConfigService.getByIcecatCategoryId(catId);
                if (vertical != null) {
                    updateVertical(category, vertical);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading category features list", e);
        }
        LOGGER.info("End loading of category features from {}", iceCatConfig.getCategoryFeatureListFileUri());
    }

    private void updateVertical(IcecatCategory category, VerticalConfig vertical) {
        Map<Integer, FeatureGroup> featureGroupById = new HashMap<>();
        if (!category.getCategoryFeatureGroups().isEmpty()) {
            for (IcecatCategoryFeatureGroup cfg : category.getCategoryFeatureGroups()) {
                int cfgId = cfg.getId();
                for (IcecatFeatureGroup ifg : cfg.getFeatureGroups()) {
                    FeatureGroup fg = vertical.getOrCreateByIceCatCategoryFeatureGroup(ifg.getId());
                    List<IcecatName> names = featureLoader.getFeatureGroupsById().get(ifg.getId()).getNames();
                    IcecatName defName = names.stream().filter(e -> e.getLangId() == 1).findFirst().orElse(null);
                    if (defName != null) {
                        fg.getName().put("default", defName.getEffectiveName());
                    }
                    IcecatName frName = names.stream().filter(e -> e.getLangId() == 3).findFirst().orElse(null);
                    if (frName != null) {
                        fg.getName().put("fr", frName.getEffectiveName());
                    }
                    FeatureGroup tmpid = featureGroupById.get(cfgId);
                    if (tmpid != null && tmpid.getIcecatCategoryFeatureGroupId() != fg.getIcecatCategoryFeatureGroupId()) {
                        LOGGER.warn("Feature group {} already present in category feature group {}", ifg.getId(), cfgId);
                    } else {
                        featureGroupById.put(cfgId, fg);
                    }
                }
            }
        }
        if (!category.getFeatures().isEmpty()) {
            for (IcecatFeature feature : category.getFeatures()) {
                int categoryFeatureGroupId = feature.getCategoryFeatureGroupId();
                FeatureGroup fg = featureGroupById.get(categoryFeatureGroupId);
                if (fg != null) {
                    Integer fId = feature.getId();
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

    public Map<Integer, IcecatCategory> getCategoriesById() {
        return categoriesById;
    }
}

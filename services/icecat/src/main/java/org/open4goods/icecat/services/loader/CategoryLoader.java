package org.open4goods.icecat.services.loader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.jaxb.Category;
import org.open4goods.icecat.jaxb.CategoryFeatureGroup;
import org.open4goods.icecat.jaxb.Feature;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final IcecatConfiguration iceCatConfig;
    private final IcecatFileDownloadService fileDownloadService;
    private final VerticalsConfigService verticalsConfigService;
    private final FeatureLoader featureLoader;

    private final Map<Integer, Category> categoriesById = new HashMap<>();

    public CategoryLoader(
            IcecatConfiguration iceCatConfig,
            IcecatFileDownloadService fileDownloadService,
            VerticalsConfigService verticalsConfigService,
            FeatureLoader featureLoader) {
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
            List<Category> categories = IcecatBulkXmlReader.readResponse(icecatFile)
                    .getCategoriesList().getCategory();
            categories.forEach(category -> categoriesById.put(IcecatBulkModelSupport.intValue(category.getID()), category));
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
     * @throws TechnicalException if the file cannot be downloaded or parsed
     */
    public void loadCategoryFeatureList() throws TechnicalException {
        if (iceCatConfig.getCategoryFeatureListFileUri() == null) {
            LOGGER.error("No category features list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getCategoryFeatureListFileUri());
        File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getCategoryFeatureListFileUri());
        try {
            LOGGER.info("Parsing {}", icecatFile);
            List<Category> categories = IcecatBulkXmlReader.readResponse(icecatFile)
                    .getCategoryFeaturesList().getCategory();
            for (Category category : categories) {
                int catId = IcecatBulkModelSupport.intValue(category.getID(), 0);
                mergeCategoryFeatureMetadata(category);
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

    private void mergeCategoryFeatureMetadata(Category category) {
        Category existing = categoriesById.get(IcecatBulkModelSupport.intValue(category.getID()));
        if (existing == null) {
            categoriesById.put(IcecatBulkModelSupport.intValue(category.getID()), category);
            return;
        }
        existing.getCategoryFeatureGroup().clear();
        existing.getCategoryFeatureGroup().addAll(category.getCategoryFeatureGroup());
        existing.getFeature().clear();
        existing.getFeature().addAll(category.getFeature());
    }

    private void updateVertical(Category category, VerticalConfig vertical) {
        Map<Integer, FeatureGroup> featureGroupById = new HashMap<>();
        if (!category.getCategoryFeatureGroup().isEmpty()) {
            for (CategoryFeatureGroup cfg : category.getCategoryFeatureGroup()) {
                int cfgId = IcecatBulkModelSupport.intValue(cfg.getID(), 0);
                for (org.open4goods.icecat.jaxb.FeatureGroup ifg : cfg.getFeatureGroup()) {
                    Integer ifgId = IcecatBulkModelSupport.intValue(ifg.getID());
                    FeatureGroup fg = vertical.getOrCreateByIceCatCategoryFeatureGroup(ifgId);
                    List<Name> names = featureLoader.getFeatureGroupsById().get(ifgId).getName();
                    Name defName = names.stream().filter(e -> IcecatBulkModelSupport.intValue(e.getLangid(), -1) == 1).findFirst().orElse(null);
                    if (defName != null) {
                        fg.getName().put("default", IcecatBulkModelSupport.effectiveName(defName));
                    }
                    Name frName = names.stream().filter(e -> IcecatBulkModelSupport.intValue(e.getLangid(), -1) == 3).findFirst().orElse(null);
                    if (frName != null) {
                        fg.getName().put("fr", IcecatBulkModelSupport.effectiveName(frName));
                    }
                    FeatureGroup tmpid = featureGroupById.get(cfgId);
                    if (tmpid != null && tmpid.getIcecatCategoryFeatureGroupId() != fg.getIcecatCategoryFeatureGroupId()) {
                        LOGGER.warn("Feature group {} already present in category feature group {}", ifgId, cfgId);
                    } else {
                        featureGroupById.put(cfgId, fg);
                    }
                }
            }
        }
        if (!category.getFeature().isEmpty()) {
            for (Feature feature : category.getFeature()) {
                int categoryFeatureGroupId = IcecatBulkModelSupport.intValue(feature.getCategoryFeatureGroupID(), 0);
                FeatureGroup fg = featureGroupById.get(categoryFeatureGroupId);
                if (fg != null) {
                    Integer fId = IcecatBulkModelSupport.intValue(feature.getID());
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

    public Map<Integer, Category> getCategoriesById() {
        return categoriesById;
    }
}

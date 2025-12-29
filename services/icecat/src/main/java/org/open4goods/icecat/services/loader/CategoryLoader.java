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
import org.open4goods.icecat.client.IcecatHttpClient;
import org.open4goods.icecat.client.exception.IcecatApiException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatCategory;
import org.open4goods.icecat.model.IcecatCategoryFeatureGroup;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureGroup;
import org.open4goods.icecat.model.IcecatModel;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Loads Icecat categories and category features from XML files.
 * Uses IcecatHttpClient for downloading and caching files.
 */
public class CategoryLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryLoader.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration icecatConfig;
    private final IcecatHttpClient httpClient;
    private final String cacheDirectory;
    private final VerticalsConfigService verticalsConfigService;
    private final FeatureLoader featureLoader;

    private final Map<Integer, IcecatCategory> categoriesById = new HashMap<>();

    /**
     * Constructor for CategoryLoader.
     *
     * @param xmlMapper               the XML mapper for parsing
     * @param icecatConfig            the Icecat configuration
     * @param httpClient              the HTTP client for file downloads
     * @param cacheDirectory          the directory for caching files
     * @param verticalsConfigService  the verticals configuration service
     * @param featureLoader           the feature loader for feature group access
     */
    public CategoryLoader(XmlMapper xmlMapper,
                          IcecatConfiguration icecatConfig,
                          IcecatHttpClient httpClient,
                          String cacheDirectory,
                          VerticalsConfigService verticalsConfigService,
                          FeatureLoader featureLoader) {
        this.xmlMapper = xmlMapper;
        this.icecatConfig = icecatConfig;
        this.httpClient = httpClient;
        this.cacheDirectory = cacheDirectory;
        this.verticalsConfigService = verticalsConfigService;
        this.featureLoader = featureLoader;
    }

    /**
     * Loads categories from the Icecat XML file.
     *
     * @throws TechnicalException if loading fails
     */
    public void loadCategories() throws TechnicalException {
        if (icecatConfig.getCategoriesListFileUri() == null) {
            LOGGER.error("No categories list file uri configured");
            return;
        }

        String uri = icecatConfig.getCategoriesListFileUri();
        LOGGER.info("Loading categories from {}", uri);

        try {
            File icecatFile = httpClient.downloadAndDecompressGzip(uri, null);
            List<IcecatCategory> categories = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getCategoryList().getCategories();

            categories.forEach(category -> categoriesById.put(category.getID(), category));

            LOGGER.info("Loaded {} categories from {}", categories.size(), uri);
        } catch (IcecatApiException e) {
            throw new TechnicalException("Failed to download categories file: " + uri, e);
        } catch (Exception e) {
            LOGGER.error("Error while loading categories", e);
            throw new TechnicalException("Error parsing categories file: " + uri, e);
        }
    }

    /**
     * Loads category feature list from the Icecat XML file.
     * Applies minification to reduce file size before parsing.
     *
     * @throws TechnicalException if loading fails
     */
    public void loadCategoryFeatureList() throws TechnicalException {
        if (icecatConfig.getCategoryFeatureListFileUri() == null) {
            LOGGER.error("No category features list file uri configured");
            return;
        }

        String uri = icecatConfig.getCategoryFeatureListFileUri();
        LOGGER.info("Loading category feature list from {}", uri);

        File minifiedFile = new File(cacheDirectory + File.separator + IdHelper.getHashedName(uri + ".min"));

        if (!minifiedFile.exists()) {
            LOGGER.info("Minified file not found, generating minified version");

            try {
                File icecatFile = httpClient.downloadAndDecompressGzip(uri, null);
                generateMinifiedFile(icecatFile, minifiedFile);
            } catch (IcecatApiException e) {
                throw new TechnicalException("Failed to download category features file: " + uri, e);
            }
        }

        try {
            LOGGER.info("Parsing minified file: {}", minifiedFile.getAbsolutePath());
            List<IcecatCategory> categories = xmlMapper.readValue(minifiedFile, IcecatModel.class)
                    .getResponse().getCategoryFeaturesList().getCategories();

            int updatedVerticals = 0;
            for (IcecatCategory category : categories) {
                int catId = category.getID();
                VerticalConfig vertical = verticalsConfigService.getByIcecatCategoryId(catId);
                if (vertical != null) {
                    updateVertical(category, vertical);
                    updatedVerticals++;
                }
            }

            LOGGER.info("Loaded {} categories, updated {} verticals from {}", categories.size(), updatedVerticals, uri);
        } catch (Exception e) {
            LOGGER.error("Error while loading category features list", e);
            throw new TechnicalException("Error parsing category features file: " + uri, e);
        }
    }

    /**
     * Generates a minified version of the category features XML file.
     * Removes Name, RestrictedValue elements and Measure blocks to reduce size.
     *
     * @param sourceFile   the source XML file
     * @param minifiedFile the destination minified file
     */
    private void generateMinifiedFile(File sourceFile, File minifiedFile) {
        LOGGER.info("Generating minified version of {}", sourceFile.getName());
        minifiedFile.delete();

        AtomicBoolean inMeasure = new AtomicBoolean(false);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(minifiedFile, true))) {
            Files.lines(sourceFile.toPath()).forEach(line -> {
                try {
                    if (line.contains("<Measure ")) {
                        inMeasure.set(true);
                    }
                    if (!inMeasure.get()) {
                        if (!line.contains("<Name") && !line.contains("<RestrictedValue")) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                    if (line.contains("</Measure")) {
                        inMeasure.set(false);
                    }
                } catch (IOException e) {
                    LOGGER.error("Error writing line to minified file", e);
                }
            });

            LOGGER.info("Generated minified file: {}", minifiedFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Error generating minified file", e);
        }
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

    public Map<Integer, IcecatCategory> getCategoriesById() {
        return categoriesById;
    }
}

package org.open4goods.icecat.services;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.icecat.client.IcecatHttpClient;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.AttributesFeatureGroups;
import org.open4goods.icecat.model.IcecatCategory;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatLanguageHandler;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * Main service for Icecat data integration.
 * Preloads Icecat data including features, feature groups, brands, categories, and languages.
 * Provides methods to resolve feature names, build attribute feature groups, and access Icecat data.
 */
public class IcecatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatService.class);

    private final IcecatHttpClient httpClient;
    private final IcecatConfiguration icecatConfig;
    private final FeatureLoader featureLoader;
    private final CategoryLoader categoryLoader;

    // Language mappings
    private Map<String, String> codeByLanguage;
    private Map<String, String> languageByCode;

    /**
     * Constructor for IcecatService.
     *
     * @param icecatConfig    the Icecat configuration
     * @param httpClient      the HTTP client for file downloads
     * @param featureLoader   the feature loader
     * @param categoryLoader  the category loader
     */
    public IcecatService(IcecatConfiguration icecatConfig,
                         IcecatHttpClient httpClient,
                         FeatureLoader featureLoader,
                         CategoryLoader categoryLoader) {
        this.icecatConfig = icecatConfig;
        this.httpClient = httpClient;
        this.featureLoader = featureLoader;
        this.categoryLoader = categoryLoader;

        try {
            icecatInit();
        } catch (TechnicalException e) {
            LOGGER.error("Error while initializing Icecat", e);
        }
    }

    /**
     * Initializes Icecat data by loading all reference data in the correct order.
     *
     * @throws TechnicalException if initialization fails
     */
    public void icecatInit() throws TechnicalException {
        LOGGER.info("Starting Icecat initialization");

        // Order matters - dependencies must be loaded first
        featureLoader.loadFeatureGroups();
        loadLanguages();
        featureLoader.loadBrands();
        categoryLoader.loadCategories();
        featureLoader.loadFeatures();
        categoryLoader.loadCategoryFeatureList();

        LOGGER.info("Icecat initialization complete");
    }

    /**
     * Loads language mappings from the Icecat XML file.
     *
     * @throws TechnicalException if loading fails
     */
    public void loadLanguages() throws TechnicalException {
        if (icecatConfig.getLanguageListFileUri() == null) {
            LOGGER.error("No language list file uri configured");
            return;
        }

        String uri = icecatConfig.getLanguageListFileUri();
        LOGGER.info("Loading languages from {}", uri);

        try {
            File icecatFile = httpClient.downloadAndDecompressGzip(uri, null);

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            IcecatLanguageHandler handler = new IcecatLanguageHandler();
            xmlReader.setContentHandler(handler);

            try (FileInputStream inputStream = new FileInputStream(icecatFile)) {
                xmlReader.parse(new InputSource(inputStream));
            }

            this.languageByCode = handler.getLanguageByCode();
            this.codeByLanguage = handler.getCodeBylanguage();

            LOGGER.info("Loaded {} languages from {}", languageByCode.size(), uri);
        } catch (Exception e) {
            LOGGER.error("Error while loading languages", e);
            throw new TechnicalException("Error loading languages: " + uri, e);
        }
    }

    /**
     * Resolves a feature name to one or more feature IDs.
     *
     * @param featureName the feature name to resolve
     * @return set of matching feature IDs, or null if not found
     */
    public Set<Integer> resolveFeatureName(String featureName) {
        String normalized = IdHelper.normalizeAttributeName(featureName);
        return featureLoader.getFeaturesByNames().get(normalized);
    }

    /**
     * Gets the localized feature name for a given feature ID and language.
     *
     * @param featureID the feature ID
     * @param language  the language code
     * @return the localized feature name, or a fallback message if not found
     */
    public String getFeatureName(Integer featureID, String language) {
        IcecatFeature feature = featureLoader.getFeaturesById().get(featureID);
        Integer icecatLanguage = getIceCatLangId(language);

        if (feature != null) {
            List<IcecatName> names = feature.getNames().getNames();
            for (IcecatName name : names) {
                if (name.getLangId() == icecatLanguage.intValue()) {
                    return name.getValue() == null ? name.getTextValue() : name.getValue();
                }
            }
        }
        return "Unsolved: " + featureID + "," + icecatLanguage;
    }

    /**
     * Converts a language code to Icecat's internal language ID.
     *
     * @param language the language code (e.g., "en", "fr")
     * @return the Icecat language ID
     */
    private Integer getIceCatLangId(String language) {
        if (languageByCode == null) {
            return 1; // Default to English
        }
        return Integer.valueOf(languageByCode.getOrDefault(language, "1"));
    }

    /**
     * Builds a list of AttributesFeatureGroups for a product based on its vertical configuration.
     *
     * @param vertical the vertical configuration
     * @param language the language code for localization
     * @param product  the product to build feature groups for
     * @return list of attribute feature groups with localized names
     */
    public List<AttributesFeatureGroups> features(VerticalConfig vertical, String language, Product product) {
        List<AttributesFeatureGroups> ret = new ArrayList<>();
        Integer icecatLanguage = getIceCatLangId(language);

        if (vertical == null) {
            return ret;
        }

        for (FeatureGroup fg : vertical.getFeatureGroups()) {
            AttributesFeatureGroups ufg = new AttributesFeatureGroups();
            ufg.setFeatureGroup(fg);
            ufg.setName(ufg.getFeatureGroup().getName().i18n(language));

            for (Integer fId : fg.getFeaturesId()) {
                ProductAttribute a = product.getAttributes().attributeByFeatureId(fId);
                if (a != null) {
                    ufg.getAttributes().add(a);

                    // Update attribute name with localized version
                    IcecatFeature f = featureLoader.getFeaturesById().get(fId);
                    if (f != null) {
                        IcecatName i18nName = f.getNames().getNames().stream()
                                .filter(e -> e.getLangId() == icecatLanguage)
                                .findFirst()
                                .orElse(null);
                        if (i18nName != null) {
                            a.setName(i18nName.getTextValue());
                        }
                    }

                    // Format multi-value attributes as HTML list
                    if (a.getValue().contains(",")) {
                        String[] values = a.getValue().split(",");
                        if (values.length > 2) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("<ul>");
                            for (String value : values) {
                                sb.append("<li>").append(value).append("</li>");
                            }
                            sb.append("</ul>");
                            a.setValue(sb.toString());
                        }
                    }
                }
            }

            if (!ufg.getAttributes().isEmpty()) {
                ret.add(ufg);
            }
        }

        return ret;
    }

    /**
     * Gets all feature IDs for a vertical configuration.
     *
     * @param vertical the vertical configuration
     * @return set of all feature IDs in the vertical
     */
    public Set<Integer> featuresId(VerticalConfig vertical) {
        Set<Integer> ret = new HashSet<>();
        if (vertical != null) {
            for (FeatureGroup fg : vertical.getFeatureGroups()) {
                ret.addAll(fg.getFeaturesId());
            }
        }
        return ret;
    }

    /**
     * Gets the feature types for a vertical configuration.
     *
     * @param vertical the vertical configuration
     * @return map of feature name to feature type
     */
    public Map<String, String> types(VerticalConfig vertical) {
        Map<String, String> ret = new HashMap<>();

        if (vertical == null) {
            return ret;
        }

        for (FeatureGroup fg : vertical.getFeatureGroups()) {
            for (Integer fId : fg.getFeaturesId()) {
                IcecatFeature f = featureLoader.getFeaturesById().get(fId);
                if (f != null) {
                    IcecatName i18nName = f.getNames().getNames().stream()
                            .filter(e -> e.getLangId() == 1)
                            .findFirst()
                            .orElse(null);

                    if (i18nName != null) {
                        ret.put(i18nName.getTextValue(), f.getType());
                    } else {
                        LOGGER.error("Name not found for feature {} - {}", fId, f);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Resolves a feature name to its canonical English name if an unambiguous match is found.
     *
     * @param name the feature name to resolve
     * @param vc   optional vertical configuration to filter by
     * @return the canonical English name, or the original name if resolution fails
     */
    public String getOriginalEnglishName(String name, VerticalConfig vc) {
        Set<Integer> featuresId = resolveFeatureName(name);

        if (featuresId == null) {
            LOGGER.warn("No icecat name found for {}", name);
            return name;
        }

        if (vc != null && vc.getId() != null) {
            featuresId = new HashSet<>(featuresId);
            featuresId.retainAll(featuresId(vc));
            if (featuresId.isEmpty()) {
                LOGGER.warn("No icecat featureID for {}, after filtering on id's for vertical {}", name, vc);
                return name;
            }
        }

        if (featuresId.size() == 1) {
            String ret = getFeatureName(featuresId.stream().findFirst().orElse(null), "en");
            LOGGER.debug("Resolved feature name: {} -> {}", name, ret);
            return ret;
        } else {
            Set<String> attrNames = featuresId.stream()
                    .map(e -> e + ":" + getFeatureName(e, "en"))
                    .collect(Collectors.toSet());
            LOGGER.warn("Conflict! attr {} can be resolved to {}", name, attrNames);
            return name;
        }
    }

    /**
     * Gets the features map by ID.
     *
     * @return map of feature ID to IcecatFeature
     */
    public Map<Integer, IcecatFeature> getFeaturesById() {
        return featureLoader.getFeaturesById();
    }

    /**
     * Sets the features map (replaces all existing entries).
     *
     * @param featuresById the new features map
     */
    public void setFeaturesById(Map<Integer, IcecatFeature> featuresById) {
        featureLoader.getFeaturesById().clear();
        featureLoader.getFeaturesById().putAll(featuresById);
    }

    /**
     * Gets the categories map by ID.
     *
     * @return map of category ID to IcecatCategory
     */
    public Map<Integer, IcecatCategory> getCategoriesById() {
        return categoryLoader.getCategoriesById();
    }

    /**
     * Sets the categories map (replaces all existing entries).
     *
     * @param categoriesById the new categories map
     */
    public void setCategoriesById(Map<Integer, IcecatCategory> categoriesById) {
        categoryLoader.getCategoriesById().clear();
        categoryLoader.getCategoriesById().putAll(categoriesById);
    }
}

package org.open4goods.icecat.services;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.AttributesFeatureGroups;
import org.open4goods.icecat.model.IcecatCategory;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatLanguageHandler;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.icecat.util.IcecatConstants;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import tools.jackson.dataformat.xml.XmlMapper;


/**
 * Core Icecat service: provides category-to-vertical mapping and product feature rendering.
 *
 * <p>Reference data (features, categories, feature groups) is loaded at startup via
 * {@link FeatureLoader} and {@link CategoryLoader}. Product feature rendering still uses
 * loaded reference data by ID. Attribute-name resolution is handled by {@link IcecatFeatureResolver}
 * through Elasticsearch.
 */
public class IcecatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatService.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration iceCatConfig;
    private final IcecatFileDownloadService fileDownloadService;
    private final FeatureLoader featureLoader;
    private final CategoryLoader categoryLoader;

    private Map<String, String> codeByLanguage;
    private Map<String, String> languageByCode;

    /**
     * Creates the IcecatService and immediately loads all reference data.
     *
     * @param xmlMapper           Jackson XML mapper (a dedicated instance, not the shared Spring one)
     * @param iceCatConfig        Icecat bulk-export configuration
     * @param fileDownloadService handles file download and decompression
     * @param featureLoader       loads features, feature groups, and suppliers
     * @param categoryLoader      loads categories and category-feature mappings
     */
    public IcecatService(
            XmlMapper xmlMapper,
            IcecatConfiguration iceCatConfig,
            IcecatFileDownloadService fileDownloadService,
            FeatureLoader featureLoader,
            CategoryLoader categoryLoader) {
        this.xmlMapper = xmlMapper;
        this.iceCatConfig = iceCatConfig;
        this.fileDownloadService = fileDownloadService;
        this.featureLoader = featureLoader;
        this.categoryLoader = categoryLoader;

        try {
            icecatInit();
        } catch (TechnicalException e) {
            LOGGER.error("Error while initializing Icecat", e);
        }
    }

    /**
     * Initialises all reference data. Order matters: feature groups must load before
     * category features (which reference them).
     *
     * @throws TechnicalException if any mandatory resource cannot be loaded
     */
    public void icecatInit() throws TechnicalException {
        featureLoader.loadFeatureGroups();
        loadLanguages();
        featureLoader.loadBrands();
        categoryLoader.loadCategories();
        featureLoader.loadFeatures();
        if (iceCatConfig.isLoadCategoryFeatureList()) {
            categoryLoader.loadCategoryFeatureList();
        } else {
            LOGGER.info("Icecat category-feature list loading is disabled");
        }
        LOGGER.info("Icecat up and running");
    }

    /**
     * Loads and parses LanguageList.xml via SAX (DOM would be too costly for this large file).
     *
     * @throws TechnicalException if the file cannot be downloaded
     */
    public void loadLanguages() throws TechnicalException {
        if (null == iceCatConfig.getLanguageListFileUri()) {
            LOGGER.error("No language list file uri configured");
            return;
        }
        LOGGER.info("Getting file from {}", iceCatConfig.getLanguageListFileUri());
        File icecatFile = fileDownloadService.getOrDownload(iceCatConfig.getLanguageListFileUri());

        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            IcecatLanguageHandler handler = new IcecatLanguageHandler();
            xmlReader.setContentHandler(handler);

            FileInputStream inputStream = new FileInputStream(icecatFile);
            xmlReader.parse(new InputSource(inputStream));

            this.languageByCode = handler.getLanguageByCode();
            this.codeByLanguage = handler.getCodeBylanguage();
        } catch (Exception e) {
            LOGGER.error("Error while loading languages", e);
        }
        LOGGER.info("End loading of languages from {}", iceCatConfig.getLanguageListFileUri());
    }

    private Integer getIceCatLangId(String language) {
        if (null == languageByCode) {
            return IcecatConstants.LANG_ID_ENGLISH;
        }
        return Integer.valueOf(languageByCode.getOrDefault(language, String.valueOf(IcecatConstants.LANG_ID_ENGLISH)));
    }

    /**
     * Loads the feature groups for a given product according to the Icecat taxonomy,
     * attaching localised attribute names from the Icecat feature registry.
     *
     * @param vertical the vertical configuration (defines which feature groups to include)
     * @param language BCP-47 language code for name localisation
     * @param product  the product whose attributes are rendered
     * @return ordered list of feature groups with their attributes
     */
    @io.micrometer.core.annotation.Timed(value = "icecat.features", description = "Time taken to resolve icecat features", extraTags = {"service", "icecat"})
    public List<AttributesFeatureGroups> features(VerticalConfig vertical, String language, Product product) {
        List<AttributesFeatureGroups> ret = new ArrayList<>();

        Integer icecatLanguage = getIceCatLangId(language);

        if (null != vertical) {
            for (FeatureGroup fg : vertical.getFeatureGroups()) {
                AttributesFeatureGroups ufg = new AttributesFeatureGroups();
                ufg.setFeatureGroup(fg);
                ufg.setName(ufg.getFeatureGroup().getName().i18n(language));
                for (Integer fId : fg.getFeaturesId()) {
                    ProductAttribute a = product.getAttributes().attributeByFeatureId(fId);
                    if (null != a) {
                        ufg.getAttributes().add(a);
                        IcecatFeature f = featureLoader.getFeaturesById().get(fId);
                        if (f != null) {
                            IcecatName i18nName = f.getNames().getNames().stream()
                                    .filter(e -> e.getLangId() == icecatLanguage)
                                    .findFirst()
                                    .orElse(null);
                            if (null != i18nName) {
                                a.setName(i18nName.getEffectiveName());
                            }
                        }

                        if (a.getValue() != null && a.getValue().contains(",")) {
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

                if (ufg.getAttributes().size() > 0) {
                    ret.add(ufg);
                }
            }
        }

        return ret;
    }

    /**
     * Returns a map of English feature name to Icecat type string for the given vertical.
     * Used by admin tooling to understand attribute types.
     *
     * @param vertical the vertical configuration
     * @return map of English name → type string (e.g. "numerical", "YES/NO")
     */
    public Map<String, String> types(VerticalConfig vertical) {
        Map<String, String> ret = new HashMap<>();
        if (null != vertical) {
            for (FeatureGroup fg : vertical.getFeatureGroups()) {
                for (Integer fId : fg.getFeaturesId()) {
                    IcecatFeature f = featureLoader.getFeaturesById().get(fId);
                    IcecatName i18nName = f.getNames().getNames().stream()
                            .filter(e -> e.getLangId() == IcecatConstants.LANG_ID_ENGLISH)
                            .findFirst()
                            .orElse(null);
                    if (null != i18nName) {
                        ret.put(i18nName.getTextValue(), f.getType());
                    } else {
                        LOGGER.error("Name not found for feature {} - {}", fId, f);
                    }
                }
            }
        }
        return ret;
    }

    public Map<Integer, IcecatFeature> getFeaturesById() {
        return featureLoader.getFeaturesById();
    }

    public void setFeaturesById(Map<Integer, IcecatFeature> featuresById) {
        featureLoader.getFeaturesById().clear();
        featureLoader.getFeaturesById().putAll(featuresById);
    }

    public Map<Integer, IcecatCategory> getCategoriesById() {
        return categoryLoader.getCategoriesById();
    }

    public void setCategoriesById(Map<Integer, IcecatCategory> categoriesById) {
        categoryLoader.getCategoriesById().clear();
        categoryLoader.getCategoriesById().putAll(categoriesById);
    }
}

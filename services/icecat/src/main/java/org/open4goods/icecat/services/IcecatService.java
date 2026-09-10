package org.open4goods.icecat.services;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.AttributesFeatureGroups;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.model.IcecatLanguageHandler;
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


/**
 * Core Icecat service: provides category-to-vertical mapping and product feature rendering.
 *
 * <p>Feature and category reference data is served from Elasticsearch through
 * {@link IcecatIndexService}, never from an in-memory map built at startup — the constructor
 * does no bulk loading. {@link FeatureLoader} and {@link CategoryLoader} are only driven by
 * {@link IcecatIndexService#syncFromLoaders()}, an explicit admin-triggered import.
 */
public class IcecatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatService.class);

    private final IcecatConfiguration iceCatConfig;
    private final IcecatFileDownloadService fileDownloadService;
    private final FeatureLoader featureLoader;
    private final CategoryLoader categoryLoader;
    private final IcecatIndexService icecatIndexService;

    private Map<String, String> codeByLanguage;
    private Map<String, String> languageByCode;

    /**
     * Creates the IcecatService and loads the small Icecat language-code table (not the
     * feature/category reference data — see {@link IcecatIndexService#syncFromLoaders()}
     * for that explicit import path).
     *
     * @param iceCatConfig        Icecat bulk-export configuration
     * @param fileDownloadService handles file download and decompression
     * @param featureLoader       loads features, feature groups, and suppliers (import path only)
     * @param categoryLoader      loads categories and category-feature mappings (import path only)
     * @param icecatIndexService  serves feature/category reads from Elasticsearch
     */
    public IcecatService(
            IcecatConfiguration iceCatConfig,
            IcecatFileDownloadService fileDownloadService,
            FeatureLoader featureLoader,
            CategoryLoader categoryLoader,
            IcecatIndexService icecatIndexService) {
        this.iceCatConfig = iceCatConfig;
        this.fileDownloadService = fileDownloadService;
        this.featureLoader = featureLoader;
        this.categoryLoader = categoryLoader;
        this.icecatIndexService = icecatIndexService;

        try {
            loadLanguages();
        } catch (TechnicalException e) {
            LOGGER.error("Error while loading Icecat languages", e);
        }
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
                        IcecatFeatureDocument f = icecatIndexService.findFeature(fId).orElse(null);
                        if (f != null) {
                            String i18nName = f.localizedName(icecatLanguage);
                            if (null != i18nName) {
                                a.setName(i18nName);
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
                    IcecatFeatureDocument f = icecatIndexService.findFeature(fId).orElse(null);
                    if (f == null) {
                        LOGGER.error("Feature {} not found in the Icecat feature index", fId);
                        continue;
                    }
                    String i18nName = f.localizedName(IcecatConstants.LANG_ID_ENGLISH);
                    if (null != i18nName) {
                        ret.put(i18nName, f.getType());
                    } else {
                        LOGGER.error("Name not found for feature {} - {}", fId, f);
                    }
                }
            }
        }
        return ret;
    }
}

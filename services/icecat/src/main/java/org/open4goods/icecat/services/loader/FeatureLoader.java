package org.open4goods.icecat.services.loader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.brand.model.Brand;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.client.IcecatHttpClient;
import org.open4goods.icecat.client.exception.IcecatApiException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureGroup;
import org.open4goods.icecat.model.IcecatModel;
import org.open4goods.icecat.model.IcecatSupplier;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Loads Icecat features, feature groups, and brands from XML files.
 * Uses IcecatHttpClient for downloading and caching files.
 */
public class FeatureLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureLoader.class);

    private final XmlMapper xmlMapper;
    private final IcecatConfiguration icecatConfig;
    private final IcecatHttpClient httpClient;
    private final BrandService brandService;

    private final Map<Integer, IcecatFeature> featuresById = new HashMap<>();
    private final Map<String, Set<Integer>> featuresByNames = new HashMap<>();
    private final Map<Integer, IcecatFeatureGroup> featureGroupsById = new HashMap<>();

    /**
     * Constructor for FeatureLoader.
     *
     * @param xmlMapper     the XML mapper for parsing
     * @param icecatConfig  the Icecat configuration
     * @param httpClient    the HTTP client for file downloads
     * @param brandService  the brand service for brand management
     */
    public FeatureLoader(XmlMapper xmlMapper,
                         IcecatConfiguration icecatConfig,
                         IcecatHttpClient httpClient,
                         BrandService brandService) {
        this.xmlMapper = xmlMapper;
        this.icecatConfig = icecatConfig;
        this.httpClient = httpClient;
        this.brandService = brandService;
    }

    /**
     * Loads features from the Icecat XML file.
     *
     * @throws TechnicalException if loading fails
     */
    public void loadFeatures() throws TechnicalException {
        if (icecatConfig.getFeaturesListFileUri() == null) {
            LOGGER.error("No features list file uri configured");
            return;
        }

        String uri = icecatConfig.getFeaturesListFileUri();
        LOGGER.info("Loading features from {}", uri);

        try {
            File icecatFile = httpClient.downloadAndDecompressGzip(uri, null);
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

            LOGGER.info("Loaded {} features from {}", features.size(), uri);
        } catch (IcecatApiException e) {
            throw new TechnicalException("Failed to download features file: " + uri, e);
        } catch (Exception e) {
            LOGGER.error("Error while loading features", e);
            throw new TechnicalException("Error parsing features file: " + uri, e);
        }
    }

    /**
     * Loads feature groups from the Icecat XML file.
     *
     * @throws TechnicalException if loading fails
     */
    public void loadFeatureGroups() throws TechnicalException {
        if (icecatConfig.getFeatureGroupsFileUri() == null) {
            LOGGER.error("No features group list file uri configured");
            return;
        }

        String uri = icecatConfig.getFeatureGroupsFileUri();
        LOGGER.info("Loading feature groups from {}", uri);

        try {
            File icecatFile = httpClient.downloadAndDecompressGzip(uri, null);
            List<IcecatFeatureGroup> groups = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getFeatureGroupsList().getFeatureGroups();

            for (IcecatFeatureGroup fg : groups) {
                featureGroupsById.put(fg.getID(), fg);
            }

            LOGGER.info("Loaded {} feature groups from {}", groups.size(), uri);
        } catch (IcecatApiException e) {
            throw new TechnicalException("Failed to download feature groups file: " + uri, e);
        } catch (Exception e) {
            LOGGER.error("Error while loading feature groups", e);
            throw new TechnicalException("Error parsing feature groups file: " + uri, e);
        }
    }

    /**
     * Loads brands from the Icecat XML file.
     *
     * @throws TechnicalException if loading fails
     */
    public void loadBrands() throws TechnicalException {
        if (icecatConfig.getBrandsListFileUri() == null) {
            LOGGER.error("No brands list file uri configured");
            return;
        }

        String uri = icecatConfig.getBrandsListFileUri();
        LOGGER.info("Loading brands from {}", uri);

        try {
            File icecatFile = httpClient.downloadAndDecompressGzip(uri, null);
            List<IcecatSupplier> suppliers = xmlMapper.readValue(icecatFile, IcecatModel.class)
                    .getResponse().getSuppliersList().getSuppliers();

            for (IcecatSupplier supplier : suppliers) {
                Brand brand = brandService.resolve(supplier.getName());
                if (brand == null) {
                    brand = new Brand();
                    brand.setBrandName(IdHelper.brandName(supplier.getName()));
                }
                // TODO: handle brand logos
            }

            LOGGER.info("Loaded {} brands from {}", suppliers.size(), uri);
        } catch (IcecatApiException e) {
            throw new TechnicalException("Failed to download brands file: " + uri, e);
        } catch (Exception e) {
            LOGGER.error("Error while loading brands", e);
            throw new TechnicalException("Error parsing brands file: " + uri, e);
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

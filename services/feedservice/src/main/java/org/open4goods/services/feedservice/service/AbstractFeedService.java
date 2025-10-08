package org.open4goods.services.feedservice.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for feed services. Provides lazy loading and caching of datasource properties,
 * and defines an abstract method to load datasource properties from an external feed source.
 */
public abstract class AbstractFeedService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // Cached datasource properties loaded from the external feed.
    // TODO (P2, design) : No scheduled reloading of feeds url
    protected Set<DataSourceProperties> datasourceCache;

    // Feed configuration (catalog URL, CSV mapping, filters, etc.)
    protected final FeedConfiguration feedConfig;

    // Service to download and cache remote files.
    protected final RemoteFileCachingService remoteFileCachingService;

    // Service to retrieve an existing datasource configuration.
    protected final DataSourceConfigService dataSourceConfigService;

    // Service to perform deep copy via JSON serialization.
    protected final SerialisationService serialisationService;

    /**
     * Constructor.
     *
     * @param feedConfig feed configuration
     * @param remoteFileCachingService service for caching remote files
     * @param dataSourceConfigService service to retrieve existing datasource configurations
     * @param serialisationService service to deep copy datasource properties
     */
    public AbstractFeedService(FeedConfiguration feedConfig, RemoteFileCachingService remoteFileCachingService,
                               DataSourceConfigService dataSourceConfigService, SerialisationService serialisationService) {
        this.feedConfig = feedConfig;
        this.remoteFileCachingService = remoteFileCachingService;
        this.dataSourceConfigService = dataSourceConfigService;
        this.serialisationService = serialisationService;
    }

    /**
     * Loads (or refreshes) the datasource cache by invoking the concrete implementation.
     */
    public synchronized void load() {
        try {
            logger.info("Loading datasource properties from catalog: {}", feedConfig.getCatalogUrl());
            Set<DataSourceProperties> initialSources = loadDatasources();
            // Filtering the excluded datasources
            datasourceCache = initialSources.stream().filter(e-> !feedConfig.getExcludeFeedKeyContains().contains(e.getFeedKey())).collect(Collectors.toSet());

            if (datasourceCache == null || datasourceCache.isEmpty()) {
                logger.warn("Datasource cache is empty after loading from catalog: {}", feedConfig.getCatalogUrl());
                // Optionally, integrate with Spring Actuator health checks here.
            }
        } catch (Exception e) {
            logger.error("Error loading datasource properties", e);
        }
    }

    /**
     * Returns the cached datasource properties, loading them if necessary.
     *
     * @return set of DataSourceProperties
     */
    public Set<DataSourceProperties> getDatasources() {
        if (datasourceCache == null) {
            load();
        }
        return datasourceCache;
    }

    /**
     * Loads datasource properties from an external feed source.
     *
     * @return set of DataSourceProperties
     * @throws Exception if an error occurs during loading
     */
    protected abstract Set<DataSourceProperties> loadDatasources() throws Exception;

    /**
     * Instantiates a volatile datasource based on the given feed key and feed URL.
     * <p>
     * If an existing datasource configuration is found for the feed key, it is deep-copied to avoid side effects.
     * Otherwise, a new datasource is created with default CSV properties from the feed configuration.
     * In both cases, the CSV datasource URLs are cleared and the provided feed URL is added.
     * </p>
     *
     * @param feedKey the unique key identifying the datasource
     * @param feedConfig the feed configuration with default CSV options
     * @param feedUrl the URL from the feed entry
     * @return a volatile DataSourceProperties instance
     * @throws Exception if cloning or instantiation fails
     */
    protected DataSourceProperties getVolatileDatasource(String feedKey, FeedConfiguration feedConfig, String feedUrl) throws Exception {
        // Retrieve any existing datasource configuration for the given feed key.
        DataSourceProperties existing = dataSourceConfigService.getDatasourcePropertiesForFeed(feedKey);
        DataSourceProperties ds;
        if (existing == null) {
            logger.info("No existing datasource found for feed key '{}'. Using default configuration.", feedKey);
            ds = new DataSourceProperties();
            ds.setCsvDatasource(feedConfig.getDefaultCsvProperties());
            ds.setDatasourceConfigName(feedKey);
        } else {
            logger.info("Existing datasource found for feed key '{}'.", feedKey);
            ds = existing;
        }

        // Create a deep copy to avoid side effects.
        DataSourceProperties volatileDs = serialisationService.fromJson(serialisationService.toJson(ds), DataSourceProperties.class);

        // Set identification details.
        if (existing == null) {
            String name = IdHelper.azCharAndDigitsPointsDash(feedKey.toLowerCase());
            volatileDs.setName(name);
        } else {
            volatileDs.setDatasourceConfigName(ds.getName());
        }

        // Clear existing CSV datasource URLs and add the provided feed URL.
        volatileDs.getCsvDatasource().getDatasourceUrls().clear();
        volatileDs.getCsvDatasource().getDatasourceUrls().add(feedUrl);

        logger.debug("Volatile datasource for key '{}' created with feed URL '{}'.", feedKey, feedUrl);
        return volatileDs;
    }

    public  String extractNameAndTld(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                // Handle cases like "example.com" without scheme
                uri = new URI("http://" + url);
                host = uri.getHost();
            }

            if (host == null) return null;

            String[] parts = host.split("\\.");
            int len = parts.length;
            if (len >= 2) {
                return parts[len - 2] + "." + parts[len - 1];
            } else {
                return host;
            }

        } catch (URISyntaxException e) {
            return null;
        }
    }
}

package org.open4goods.services.feedservice.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.model.AwinMerchant;
import org.open4goods.services.remotefilecaching.config.CacheResourceConfig;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Feed service implementation for Awin.
 * <p>
 * This service loads a CSV catalog file (cached on disk) and enhances each datasource
 * with additional metadata from the Awin API. It uses the getVolatileDatasource() method
 * (inherited from AbstractFeedService) to create or clone datasources with proper CSV parsing options.
 * </p>
 */
@Service
public class AwinFeedService extends AbstractFeedService {

    private final Logger logger = LoggerFactory.getLogger(AwinFeedService.class);

    // CSV mapper for parsing the catalog file.
    private final CsvMapper csvMapper;

    // ObjectMapper for JSON processing.
    private final ObjectMapper objectMapper;

    // Placeholder Awin API credentials; in production these would be injected via configuration.
    private final String awinAccessToken;
    private final String advertiserId;

    /**
     * Constructor.
     *
     * @param feedConfig the Awin-specific feed configuration
     * @param remoteFileCachingService service to cache remote files
     * @param dataSourceConfigService service to retrieve existing datasource properties
     * @param serialisationService service to deep copy datasource properties
     */
    public AwinFeedService(FeedConfiguration feedConfig,
                           RemoteFileCachingService remoteFileCachingService,
                           DataSourceConfigService dataSourceConfigService,
                           SerialisationService serialisationService,
                           String advertiserId,
                           String awinAccessToken
                           ) {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.csvMapper = new CsvMapper();
        // Optionally register modules or configure mapper settings.
        csvMapper.findAndRegisterModules();
        this.objectMapper = new ObjectMapper();
        // Placeholder values; these should be provided via external configuration.
        this.awinAccessToken = awinAccessToken;
        this.advertiserId = advertiserId;
    }

    /**
     * Scheduled refresh of Awin datasource properties according to configuration.
     */
    @Scheduled(cron = "${feed.awin.cron}")
    public void scheduledLoad() {
        if (!feedConfig.getAwin().isEnabled()) {
            logger.info("Awin feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of Awin datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception {
        Set<DataSourceProperties> result = new HashSet<>();

        // Use RemoteFileCachingService to download and cache the CSV catalog file (with a one-day refresh).
        CacheResourceConfig cacheConfig = new CacheResourceConfig();
        cacheConfig.setUrl(feedConfig.getCatalogUrl());
        cacheConfig.setRefreshInDays(1);
        File cachedCatalog = remoteFileCachingService.retrieve(cacheConfig);

        // Define CSV schema based on the feed configuration (header, comma separator, quoted values).
        CsvSchema schema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnSeparator(',')
                .withQuoteChar('"');
        ObjectReader oReader = csvMapper.readerFor(HashMap.class).with(schema);

        MappingIterator<Map<String, String>> it = oReader.readValues(cachedCatalog);
        // Retrieve additional metadata from the Awin API and update datasource properties.
        List<AwinMerchant> merchants = retrieveAwinMerchantMetadata(awinAccessToken, advertiserId);

        // Optimization: Index merchants by ID and Name for O(1) lookup
        Map<Integer, AwinMerchant> merchantsById = merchants.stream()
                .collect(Collectors.toMap(AwinMerchant::getId, Function.identity(), (existing, replacement) -> existing));
        Map<String, AwinMerchant> merchantsByName = merchants.stream()
                .filter(m -> m.getName() != null)
                .collect(Collectors.toMap(m -> m.getName().toLowerCase(), Function.identity(), (existing, replacement) -> existing));


        while (it.hasNext()) {
            Map<String, String> line = it.next();

            //TODO : from conf
            if (line.containsKey("Membership Status") && !line.get("Membership Status").equalsIgnoreCase("Active")) {
            	continue;
            }
            // Retrieve the feed key using the attribute defined in the feed configuration.
            String feedKey = line.get(feedConfig.getDatasourceKeyAttribute());
            Integer programId = null;
            try {
                programId = Integer.valueOf(line.get("Advertiser ID"));
            } catch (NumberFormatException e) {
                // ignore
            }
            
            if (feedKey == null || feedKey.trim().isEmpty()) {
                logger.warn("Skipping CSV line due to missing feed key: {}", line);
                continue;
            }

            // (Optional) Apply filtering logic based on feedConfig.filterAttributes and excludeFeedKeyContains.
            String feedUrl = line.get(feedConfig.getDatasourceUrlAttribute());

            // Instantiate a volatile datasource (cloning an existing one or creating a new one).
            DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);

            // Optionally set additional CSV parsing options (e.g. language).
            if (feedConfig.getDatasourceLanguageAttribute() != null) {
                String language = line.get(feedConfig.getDatasourceLanguageAttribute());
                ds.setLanguage(language);
            }

            if (!merchants.isEmpty()) {
                AwinMerchant merchant = null;
                if (programId != null) {
                    merchant = merchantsById.get(programId);
                }
                if (merchant == null) {
                    merchant = merchantsByName.get(feedKey.toLowerCase());
                }

                if (null != merchant ) {
                    ds.setDatasourceConfigName(merchant.getName());

                    ds.setLogo(merchant.getLogoUrl());
                    ds.setPortalUrl(merchant.getDisplayUrl());
                    ds.setAffiliatedPortalUrl(merchant.getClickThroughUrl());
                    ds.setName(extractNameAndTld(ds.getPortalUrl()));
                    if (merchant.getPrimaryRegion() != null && merchant.getPrimaryRegion().getCountryCode() != null) {
                       ds.setLanguage(merchant.getPrimaryRegion().getCountryCode().substring(0,2).toLowerCase());
                    }
                    ds.setDescription(merchant.getDescription());
                }
            } else {
                logger.warn("No Awin merchant metadata found for feed key '{}'.", feedKey);
            }

            result.add(ds);
        }
        logger.info("Awin datasources loaded: {} entries", result.size());
        return result;
    }

    /**
     * Retrieves Awin merchant metadata using file caching.
     * <p>
     * The API response is cached on disk (using RemoteFileCachingService) for one day.
     * </p>
     *
     * @param accessToken the Awin API access token
     * @param advertiserId the advertiser identifier
     * @return a list of AwinMerchant objects containing metadata
     * @throws Exception if the API call or parsing fails
     */
    public List<AwinMerchant> retrieveAwinMerchantMetadata(String accessToken, String advertiserId) throws Exception {
        String endpoint = "https://api.awin.com/publishers/" + advertiserId
                + "/programmes?relationship=joined&accessToken=" + accessToken;
        logger.info("Retrieving Awin merchant metadata from endpoint: {}", endpoint);

        int cacheTtl = feedConfig.getAwin().getCacheTtlDays();
        File cachedResponse = remoteFileCachingService.getResource(endpoint, cacheTtl);

        if (cachedResponse == null || !cachedResponse.exists()) {
            throw new Exception("Cached response file not found for Awin merchant metadata");
        }

        // Read the file content as a UTF-8 string.
        String jsonResponse = new String(Files.readAllBytes(cachedResponse.toPath()), StandardCharsets.UTF_8);
        // Parse the JSON response to a list of AwinMerchant objects.
        List<AwinMerchant> merchants = objectMapper.readValue(jsonResponse, new TypeReference<List<AwinMerchant>>() {});
        if (merchants == null || merchants.isEmpty()) {
            throw new Exception("No merchants found for advertiserId: " + advertiserId);
        }
        logger.info("Retrieved {} Awin merchant metadata entries", merchants.size());
        return merchants;
    }
}

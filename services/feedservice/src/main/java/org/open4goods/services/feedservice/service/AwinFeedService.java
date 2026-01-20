package org.open4goods.services.feedservice.service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.dto.AffiliationKpis;
import org.open4goods.services.feedservice.dto.AffiliationKpis.AffiliationKpisBreakdown;
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
import com.fasterxml.jackson.databind.JsonNode;

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
     * Scheduled refresh of Awin datasource properties every day at a random time.
     */
    @Scheduled(cron = "0 "
            + "#{T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,60)} "
            + "#{T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,24)} * * ?")
    public void scheduledLoad() {
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

        while (it.hasNext()) {
            Map<String, String> line = it.next();

            //TODO : from conf
            if (!line.get("Membership Status").equalsIgnoreCase("Active")) {
            	continue;
            }
            // Retrieve the feed key using the attribute defined in the feed configuration.
            String feedKey = line.get(feedConfig.getDatasourceKeyAttribute());
            Integer programId = Integer.valueOf(line.get("Advertiser ID"));
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
            	//TODO(performance) : should use a map
                AwinMerchant merchant = merchants.stream().filter(e-> (e.getId() == (programId)) || (e.getName().equalsIgnoreCase(feedKey)) ).findFirst().orElse(null);
                if (null != merchant ) {
                ds.setDatasourceConfigName(merchant.getName());

                ds.setLogo(merchant.getLogoUrl());
                ds.setPortalUrl(merchant.getDisplayUrl());
                ds.setAffiliatedPortalUrl(merchant.getClickThroughUrl());
                ds.setName(extractNameAndTld(ds.getPortalUrl()));
                ds.setLanguage(merchant.getPrimaryRegion().getCountryCode().substring(0,2).toLowerCase());
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

        // TODO(p3,conf) : refresh in days from conf
        File cachedResponse = remoteFileCachingService.getResource(endpoint,1);

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

    /**
     * Retrieves aggregated KPIs for a date range.
     *
     * @param start start date (inclusive)
     * @param end end date (inclusive)
     * @return aggregated KPIs
     * @throws Exception when the API call fails
     */
    public AffiliationKpis getKpis(LocalDate start, LocalDate end) throws Exception {
        if (awinAccessToken == null || awinAccessToken.isBlank()) {
            throw new IllegalStateException("Awin access token is missing");
        }
        if (advertiserId == null || advertiserId.isBlank()) {
            throw new IllegalStateException("Awin advertiser id is missing");
        }
        JsonNode root = retrieveAwinPerformance(start, end);
        JsonNode rows = extractRows(root);
        if (rows == null || !rows.isArray()) {
            throw new IllegalStateException("Awin performance payload is missing rows");
        }

        long clicks = 0;
        long impressions = 0;
        long transactionsTotal = 0;
        long transactionsConfirmed = 0;
        long transactionsPending = 0;
        BigDecimal commissionTotal = BigDecimal.ZERO;
        BigDecimal turnoverTotal = BigDecimal.ZERO;
        Map<String, AffiliationKpisBreakdown> breakdown = new HashMap<>();

        for (JsonNode row : rows) {
            long rowClicks = row.path("clicks").asLong(0);
            long rowImpressions = row.path("impressions").asLong(0);
            long rowTransactions = row.path("transactions").asLong(row.path("transactionsTotal").asLong(0));
            long rowConfirmed = row.path("transactionsConfirmed")
                    .asLong(row.path("validatedTransactions").asLong(0));
            long rowPending = row.path("transactionsPending").asLong(row.path("pendingTransactions").asLong(0));
            BigDecimal rowCommission = readDecimal(row, "commission", "commissionAmount");
            BigDecimal rowTurnover = readDecimal(row, "turnover", "saleAmount");

            clicks += rowClicks;
            impressions += rowImpressions;
            transactionsTotal += rowTransactions;
            transactionsConfirmed += rowConfirmed;
            transactionsPending += rowPending;
            commissionTotal = commissionTotal.add(rowCommission);
            turnoverTotal = turnoverTotal.add(rowTurnover);

            Optional<String> programName = resolveProgramName(row);
            if (programName.isPresent()) {
                breakdown.put(programName.get(), new AffiliationKpisBreakdown(
                        rowClicks,
                        rowImpressions,
                        rowTransactions,
                        rowConfirmed,
                        rowPending,
                        rowCommission,
                        rowTurnover
                ));
            }
        }

        return new AffiliationKpis(
                clicks,
                impressions,
                transactionsTotal,
                transactionsConfirmed,
                transactionsPending,
                commissionTotal,
                turnoverTotal,
                breakdown.isEmpty() ? null : breakdown
        );
    }

    private JsonNode retrieveAwinPerformance(LocalDate start, LocalDate end) throws Exception {
        String endpoint = "https://api.awin.com/publishers/" + advertiserId
                + "/reports/publisher/advertiser?startDate=" + start
                + "&endDate=" + end
                + "&accessToken=" + awinAccessToken;
        logger.info("Retrieving Awin performance from endpoint: {}", endpoint);
        File cachedResponse = remoteFileCachingService.getResource(endpoint, 1);
        if (cachedResponse == null || !cachedResponse.exists()) {
            throw new Exception("Awin performance cached response file not found");
        }
        String jsonResponse = new String(Files.readAllBytes(cachedResponse.toPath()), StandardCharsets.UTF_8);
        return objectMapper.readTree(jsonResponse);
    }

    private JsonNode extractRows(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("data")) {
            return root.path("data");
        }
        if (root.has("rows")) {
            return root.path("rows");
        }
        return null;
    }

    private Optional<String> resolveProgramName(JsonNode row) {
        String name = row.path("advertiser").asText(null);
        if (name == null || name.isBlank()) {
            name = row.path("advertiserName").asText(null);
        }
        return Optional.ofNullable(name);
    }

    private BigDecimal readDecimal(JsonNode row, String primary, String secondary) {
        if (row.hasNonNull(primary)) {
            return new BigDecimal(row.path(primary).asText("0"));
        }
        if (row.hasNonNull(secondary)) {
            return new BigDecimal(row.path(secondary).asText("0"));
        }
        return BigDecimal.ZERO;
    }
}

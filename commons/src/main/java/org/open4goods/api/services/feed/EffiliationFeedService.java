package org.open4goods.api.services.feed;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.services.remotefilecaching.config.CacheResourceConfig;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Feed service implementation for Effiliation.
 * This implementation uses a REST API call to fetch JSON feed data and maps it to datasource properties.
 * The getVolatileDatasource method is used to ensure consistent CSV parsing options.
 */
@Service
public class EffiliationFeedService extends AbstractFeedService {

    private final Logger logger = LoggerFactory.getLogger(EffiliationFeedService.class);
    
    private final ObjectMapper objectMapper;
    // Placeholder Effiliation API key; should be injected from external configuration.
    private final String effiliationApiKey;
    
    /**
     * Constructor.
     *
     * @param feedConfig Effiliation feed configuration
     * @param remoteFileCachingService service for caching remote files
     * @param dataSourceConfigService service to retrieve existing datasource properties
     * @param serialisationService service to deep copy datasource properties
     */
    public EffiliationFeedService(FeedConfiguration feedConfig, RemoteFileCachingService remoteFileCachingService,
                                  DataSourceConfigService dataSourceConfigService, SerialisationService serialisationService, String effiliationApiKey) {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.objectMapper = new ObjectMapper();
        this.effiliationApiKey = effiliationApiKey;
    }
    
    /**
     * Scheduled refresh of Effiliation datasource properties every day at a random time (with a 30-second offset).
     */
    @Scheduled(cron = "30 " 
            + "#{T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,60)} " 
            + "#{T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,24)} * * ?")
    public void scheduledLoad() {
        logger.info("Scheduled refresh of Effiliation datasources initiated.");
        load();
    }
    
    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception {
        Set<DataSourceProperties> result = new HashSet<>();
        
        // Retrieve feed data via Effiliation REST API.
        JsonNode root = retrieveEffiliationFeeds(effiliationApiKey, null);
        if (root != null && root.has("feeds")) {
            for (JsonNode feedNode : root.get("feeds")) {
                // Use the "nom" field as the feed key.
                String feedKey = feedNode.path("nom").asText();
                if (feedKey == null || feedKey.trim().isEmpty()) {
                    logger.warn("Skipping Effiliation feed entry due to missing 'nom': {}", feedNode);
                    continue;
                }
                String feedUrl = feedNode.path("code").asText();
                // Instantiate a volatile datasource with proper CSV parsing settings.
                DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);
                
                // Set additional properties from the Effiliation JSON.
                ds.setPortalUrl(feedNode.path("url_affilieur").asText());
                ds.setLogo(feedNode.path("url_logo").asText());
                ds.setFavico(feedNode.path("url_logo").asText());
                
                result.add(ds);
            }
        } else {
            logger.warn("No 'feeds' node found in Effiliation API response.");
        }
        logger.info("Effiliation datasources loaded: {} entries", result.size());
        return result;
    }
    
    /**
     * Retrieves feed data from the Effiliation REST API.
     *
     * @param apiKey Effiliation API key
     * @param merchantId (optional) merchant identifier
     * @return a JsonNode representing the API response
     * @throws Exception if the API call fails
     */
    public JsonNode retrieveEffiliationFeeds(String apiKey, String merchantId) throws Exception {
        String endpoint = "https://apiv2.effiliation.com/apiv2/productfeeds.json?filter=mines&lg=fr&key=" + apiKey;
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(35000);
        connection.setReadTimeout(35000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to retrieve Effiliation feeds, response code: " + responseCode);
        }
        return objectMapper.readTree(connection.getInputStream());
    }
}

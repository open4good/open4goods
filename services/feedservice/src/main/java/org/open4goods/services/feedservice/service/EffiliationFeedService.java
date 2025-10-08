package org.open4goods.services.feedservice.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.model.EffiliationProgram;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Feed service implementation for Effiliation.
 * This implementation uses a REST API call to fetch JSON feed data and maps it to datasource properties.
 * The API response is cached using RemoteFileCachingService to avoid repeated downloads.
 */
@Service
public class EffiliationFeedService extends AbstractFeedService {



	private final Logger logger = LoggerFactory.getLogger(EffiliationFeedService.class);

    private final ObjectMapper objectMapper;
    private final String effiliationApiKey;

    /**
     * Constructor.
     *
     * @param feedConfig Effiliation feed configuration
     * @param remoteFileCachingService service for caching remote files
     * @param dataSourceConfigService service to retrieve existing datasource properties
     * @param serialisationService service to deep copy datasource properties
     * @param effiliationApiKey the Effiliation API key
     */
    public EffiliationFeedService(FeedConfiguration feedConfig,
                                   RemoteFileCachingService remoteFileCachingService,
                                   DataSourceConfigService dataSourceConfigService,
                                   SerialisationService serialisationService,
                                   String effiliationApiKey) {
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

        // Retrieve complete programs
        Map<String,EffiliationProgram> programs = retrieveEffiliationPrograms(effiliationApiKey);


        // Retrieve feed data via Effiliation REST API with caching
        JsonNode root = retrieveEffiliationFeeds(effiliationApiKey);
        if (root != null && root.has("feeds")) {
            for (JsonNode feedNode : root.get("feeds")) {
                String feedKey = feedNode.path("nom").asText();
                if (feedKey == null || feedKey.trim().isEmpty()) {
                    logger.warn("Skipping Effiliation feed entry due to missing 'nom': {}", feedNode);
                    continue;
                }

                String feedUrl = feedNode.path("code").asText();
                DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);

                String idaff = feedNode.path("id_affilieur").asText();
                EffiliationProgram p = programs.get(idaff);

				if (null == p) {
					logger.error("Program with affiliation id {} not found", idaff);
				} else {

					ds.setPortalUrl(feedNode.path("url_affilieur").asText());
					ds.setAffiliatedPortalUrl(p.getUrlTracke());
					ds.setLogo(p.getUrlLogo());
					// TODO : Set language from p.getCountry
					ds.setName(extractNameAndTld(ds.getPortalUrl()));

					result.add(ds);
				}
            }
        } else {
            logger.warn("No 'feeds' node found in Effiliation API response.");
        }

        logger.info("Effiliation datasources loaded: {} entries", result.size());
        return result;
    }


	private Map<String, EffiliationProgram> retrieveEffiliationPrograms(String effiliationApiKey) {
	    String endpoint = "https://apiv2.effiliation.com/apiv2/programs.json?filter=mines&lg=fr&key=" + effiliationApiKey;
	    Map<String, EffiliationProgram> programs = new HashMap<>();
	    ObjectMapper mapper = new ObjectMapper();

	    try {

	    	  // TODO(p3,conf) : refresh in days from conf
	        File cachedResponse = remoteFileCachingService.getResource(endpoint,1);
	        if (cachedResponse == null || !cachedResponse.exists()) {
	            throw new Exception("Effiliation cached response file not found.");
	        }

	        String jsonResponse = new String(Files.readAllBytes(cachedResponse.toPath()), StandardCharsets.UTF_8);


	        JsonNode root = mapper.readTree(jsonResponse);
	        JsonNode programsNode = root.path("programs");

	        if (programsNode.isArray()) {
	            List<EffiliationProgram> programList = mapper.convertValue(programsNode, new TypeReference<>() {});
	            for (EffiliationProgram p : programList) {
	                if (p.getIdAffilieur() != null) {
	                    programs.put(String.valueOf(p.getIdAffilieur()), p);
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return programs;
	}

	/**
     * Retrieves Effiliation feed metadata using a cached API response.
     * The response is cached on disk using the RemoteFileCachingService and refreshed every day.
     *
     * @param apiKey the Effiliation API key
     * @return JsonNode representing the response, or null if retrieval fails
     * @throws Exception if the cache or deserialization fails
     */
    public JsonNode retrieveEffiliationFeeds(String apiKey) throws Exception {
    	// TODO (P3, i18n) : have to localize parameter to get right descriptions
        String endpoint = "https://apiv2.effiliation.com/apiv2/productfeeds.json?filter=mines&lg=fr&key=" + apiKey;

        logger.info("Retrieving Effiliation feed metadata from endpoint: {}", endpoint);



        // TODO(p3,conf) : refresh in days from conf
        File cachedResponse = remoteFileCachingService.getResource(endpoint,1);
        if (cachedResponse == null || !cachedResponse.exists()) {
            throw new Exception("Effiliation cached response file not found.");
        }

        String jsonResponse = new String(Files.readAllBytes(cachedResponse.toPath()), StandardCharsets.UTF_8);
        return objectMapper.readTree(jsonResponse);
    }
}

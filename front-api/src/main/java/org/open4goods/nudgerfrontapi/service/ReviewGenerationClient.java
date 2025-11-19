package org.open4goods.nudgerfrontapi.service;

import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.nudgerfrontapi.config.properties.ReviewGenerationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * HTTP client responsible for bridging review generation requests to the
 * back-office API.
 */
@Service
public class ReviewGenerationClient {

    /**
     * JsonMapper configured to allow missing creator properties.
     * This is intentional and necessary because the ReviewGenerationStatus response
     * may contain optional fields that are not always present. Setting
     * FAIL_ON_MISSING_CREATOR_PROPERTIES to false ensures deserialization succeeds
     * even when some properties are absent in the JSON response.
     * This configuration is working as expected and should not be changed.
     */
    private static JsonMapper jsonBuilder = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .build();

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewGenerationClient.class);

    private final RestClient restClient;
    private final ReviewGenerationProperties properties;

    public ReviewGenerationClient(RestClient.Builder restClientBuilder, ReviewGenerationProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl())
                .defaultHeader(UrlConstants.APIKEY_PARAMETER, properties.getApiKey())
                .build();
    }

    /**
     * Trigger review generation for the provided UPC.
     *
     * @param upc product identifier forwarded to the back-office API
     * @return echoed UPC once the job is scheduled
     */
    public long triggerGeneration(long upc) {
        Long response = restClient.post()
                .uri(builder -> builder.path(properties.getReviewPath()).path("/{id}").build(upc))
                .retrieve()
                .body(Long.class);
        return response == null ? upc : response;
    }

    /**
     * Retrieve the review generation status associated with the provided UPC.
     *
     * @param upc product identifier used to request the status
     * @return latest status snapshot or {@code null} when the back-office has no
     *         entry for the UPC
     */
    public ReviewGenerationStatus getStatus(long upc) {
        try {
            String body = restClient.get()
                    .uri(builder -> builder.path(properties.getReviewPath()).path("/{id}").build(upc))
                    .retrieve()
                    .body(String.class);

            ReviewGenerationStatus response = jsonBuilder.readValue(body, ReviewGenerationStatus.class);

            return response;

        } catch (Exception e) {

            LOGGER.error("Error while retrieving ReviewGenerationStatus ", e);
        }

        return null;
    }
}

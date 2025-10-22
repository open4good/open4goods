package org.open4goods.nudgerfrontapi.service;

import org.open4goods.model.constants.UrlConstants;
import org.open4goods.nudgerfrontapi.config.properties.ReviewGenerationProperties;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * HTTP client responsible for bridging review generation requests to the back-office API.
 */
@Service
public class ReviewGenerationClient {

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
     * @return latest status snapshot or {@code null} when the back-office has no entry for the UPC
     */
    public ReviewGenerationStatus getStatus(long upc) {
        return restClient.get()
                .uri(builder -> builder.path(properties.getReviewPath()).path("/{id}").build(upc))
                .retrieve()
                .body(ReviewGenerationStatus.class);
    }
}

package org.open4goods.nudgerfrontapi.service.exposed;

import java.util.List;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.nudgerfrontapi.config.properties.ExposedDocsProperties;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsContentDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsOverviewDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsSearchResultDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsTreeNodeDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service proxying exposed documentation endpoints from the dedicated microservice.
 */
@Service
public class ExposedDocsService
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedDocsService.class);

    private final RestClient restClient;
    private final ExposedDocsProperties properties;

    /**
     * Builds a proxy service for the exposed docs microservice.
     *
     * @param restClientBuilder REST client builder
     * @param properties configuration properties
     */
    public ExposedDocsService(RestClient.Builder restClientBuilder, ExposedDocsProperties properties)
    {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    /**
     * Retrieves the overview of exposed resource categories.
     *
     * @param domainLanguage localisation hint
     * @return overview of categories
     * @throws ResourceNotFoundException when the endpoint is unavailable
     */
    public ExposedDocsOverviewDto getOverview(DomainLanguage domainLanguage) throws ResourceNotFoundException
    {
        return get("/exposed", ExposedDocsOverviewDto.class, "overview");
    }

    /**
     * Retrieves the resource tree for a category.
     *
     * @param categoryId category identifier
     * @param domainLanguage localisation hint
     * @return resource tree
     * @throws ResourceNotFoundException when the category is missing
     */
    public ExposedDocsTreeNodeDto getTree(String categoryId, DomainLanguage domainLanguage) throws ResourceNotFoundException
    {
        return get("/exposed/" + categoryId, ExposedDocsTreeNodeDto.class, "category tree");
    }

    /**
     * Retrieves the content for a single resource.
     *
     * @param categoryId category identifier
     * @param path resource path
     * @param domainLanguage localisation hint
     * @return resource content
     * @throws ResourceNotFoundException when the resource is missing
     */
    public ExposedDocsContentDto getContent(String categoryId, String path, DomainLanguage domainLanguage) throws ResourceNotFoundException
    {
        String uri = UriComponentsBuilder.fromPath("/exposed/{categoryId}/content")
                .queryParam("path", path)
                .buildAndExpand(categoryId)
                .toUriString();
        return get(uri, ExposedDocsContentDto.class, "content");
    }

    /**
     * Searches embedded resources.
     *
     * @param query query text
     * @param categories categories to limit the search
     * @param pathPrefix optional path prefix
     * @param searchContent whether to search file contents
     * @param includeContent whether to include content in results
     * @param domainLanguage localisation hint
     * @return search results
     * @throws ResourceNotFoundException when the search endpoint is unavailable
     */
    public List<ExposedDocsSearchResultDto> search(String query,
                                                   List<String> categories,
                                                   String pathPrefix,
                                                   boolean searchContent,
                                                   boolean includeContent,
                                                   DomainLanguage domainLanguage) throws ResourceNotFoundException
    {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/exposed/search")
                .queryParam("query", query)
                .queryParam("searchContent", searchContent)
                .queryParam("includeContent", includeContent);
        if (pathPrefix != null) {
            builder.queryParam("pathPrefix", pathPrefix);
        }
        if (categories != null) {
            for (String category : categories) {
                builder.queryParam("categories", category);
            }
        }

        try {
            return restClient.get()
                    .uri(builder.toUriString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException e) {
            throw mapException("search", e);
        }
    }

    /**
     * Executes a GET request for a typed response.
     *
     * @param path request path
     * @param type response type
     * @param label label for error handling
     * @param <T> response type
     * @return parsed response body
     * @throws ResourceNotFoundException when the endpoint returns 404
     */
    private <T> T get(String path, Class<T> type, String label) throws ResourceNotFoundException
    {
        try {
            return restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException e) {
            throw mapException(label, e);
        }
    }

    /**
     * Maps HTTP errors to domain exceptions for the front API.
     *
     * @param label label describing the request
     * @param e response exception thrown by the client
     * @return runtime exception to throw
     * @throws ResourceNotFoundException when the remote endpoint returns 404
     */
    private RuntimeException mapException(String label, RestClientResponseException e) throws ResourceNotFoundException
    {
        LOGGER.warn("Exposed docs {} request failed with status {}", label, e.getStatusCode());
        if (e.getStatusCode().value() == 404) {
            throw new ResourceNotFoundException("Exposed docs " + label + " not found");
        }
        return new IllegalStateException("Failed to fetch exposed docs " + label + " from " + properties.getBaseUrl(), e);
    }
}

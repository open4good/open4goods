package org.open4goods.eprelservice.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.open4goods.eprelservice.config.EprelServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RequestCallback;

/**
 * Default {@link EprelApiClient} relying on {@link RestTemplate}.
 */
@Component
public class RestEprelApiClient implements EprelApiClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RestEprelApiClient.class);

    private static final ParameterizedTypeReference<List<EprelProductGroup>> PRODUCT_GROUP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>()
            {
            };

    private final RestTemplate restTemplate;
    private final EprelServiceProperties properties;

    /**
     * Creates the client.
     *
     * @param restTemplate HTTP client
     * @param properties   EPREL configuration
     */
    public RestEprelApiClient(RestTemplate restTemplate, EprelServiceProperties properties)
    {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public List<EprelProductGroup> fetchProductGroups()
    {
        try
        {
            String url = properties.getApiUrl() + "/product-groups";
            List<EprelProductGroup> response = restTemplate.exchange(url, HttpMethod.GET, null,
                    PRODUCT_GROUP_RESPONSE_TYPE).getBody();
            if (CollectionUtils.isEmpty(response))
            {
                return List.of();
            }
            return List.copyOf(response);
        }
        catch (RestClientException e)
        {
            LOGGER.error("Unable to fetch EPREL product groups", e);
            return List.of();
        }
    }

    @Override
    public Path downloadCatalogueZip(String urlCode) throws IOException
    {
        Path tempFile = Files.createTempFile("eprel-catalogue-" + urlCode + "-", ".zip");
        try
        {
            String url = properties.getApiUrl() + "/exportProducts/" + urlCode;
            RequestCallback requestCallback = request -> request.getHeaders().add("x-api-key", properties.getApiKey());
            restTemplate.execute(url, HttpMethod.GET, requestCallback, response ->
            {
                try
                {
                    verifySuccessful(response, urlCode);
                    try (InputStream body = response.getBody())
                    {
                        if (body == null)
                        {
                            throw new IOException("EPREL response body is empty");
                        }
                        Files.copy(body, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                catch (IOException ioException)
                {
                    throw new UncheckedIOException(ioException);
                }
                return null;
            });
            return tempFile;
        }
        catch (RestClientException | UncheckedIOException e)
        {
            LOGGER.error("Unable to download EPREL catalogue for {}", urlCode, e);
            Files.deleteIfExists(tempFile);
            throw new IOException("Failed to download EPREL catalogue", e);
        }
    }

    private void verifySuccessful(ClientHttpResponse response, String urlCode) throws IOException
    {
        if (response.getStatusCode().is2xxSuccessful())
        {
            return;
        }
        LOGGER.error("EPREL catalogue download for {} failed with status {}", urlCode, response.getStatusCode());
        if (response.getStatusCode().is4xxClientError())
        {
            List<String> remainingHeaders = response.getHeaders().get(HttpHeaders.RETRY_AFTER);
            if (!CollectionUtils.isEmpty(remainingHeaders))
            {
                LOGGER.warn("Retry-After headers received: {}", Arrays.toString(remainingHeaders.toArray()));
            }
        }
        throw new IOException("Unexpected EPREL response status: " + response.getStatusCode());
    }
}

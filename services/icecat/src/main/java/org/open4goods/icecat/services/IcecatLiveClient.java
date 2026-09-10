package org.open4goods.icecat.services;

import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatLiveApiResponse;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.model.localization.DomainLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Live client for the Icecat product API: owns HTTP transport, tolerant JSON parsing and
 * mapping the raw response into the neutral {@link IcecatLiveLookupResult}.
 *
 * <p>Product orchestration (deciding when to call, converting a found product into a
 * {@code DataFragment}, persisting it) stays in {@code api}'s completion service, which acts
 * as a coordinator over this client.
 */
public class IcecatLiveClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatLiveClient.class);

    private final IcecatCompletionConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IcecatLiveClient(IcecatCompletionConfig config) {
        this(config, buildRestClient(config), new ObjectMapper());
    }

    IcecatLiveClient(IcecatCompletionConfig config, RestClient restClient, ObjectMapper objectMapper) {
        this.config = config;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    private static RestClient buildRestClient(IcecatCompletionConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs());
        factory.setReadTimeout(config.getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Fetches and parses a single product from the Icecat live API, by GTIN (search).
     *
     * @param gtin     the product's GTIN
     * @param language the domain language to request localized content in
     * @return the neutral lookup outcome; never throws for expected HTTP/parsing failures
     */
    public IcecatLiveLookupResult fetchProduct(long gtin, DomainLanguage language) {
        return fetch(buildUrl(gtin, language), "gtin " + gtin);
    }

    /**
     * Fetches and parses a single product from the Icecat live API, by its Icecat product id
     * (refresh of a product already matched to Icecat).
     *
     * @param icecatId the Icecat product id previously matched via {@link #fetchProduct(long, DomainLanguage)}
     * @param language the domain language to request localized content in
     * @return the neutral lookup outcome; never throws for expected HTTP/parsing failures
     */
    public IcecatLiveLookupResult fetchProductByIcecatId(String icecatId, DomainLanguage language) {
        return fetch(buildIdUrl(icecatId, language), "icecatId " + icecatId);
    }

    private IcecatLiveLookupResult fetch(String url, String label) {
        int maxAttempts = Math.max(1, config.getMaxRetryAttempts());
        LOGGER.info("Loading icecat data {}", url);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String content = restClient.get().uri(url).retrieve().body(String.class);
                IceDataItem item = objectMapper.readValue(content, IcecatLiveApiResponse.class).data;
                if (item == null || item.generalInfo == null) {
                    LOGGER.warn("Icecat response for {} does not contain product data", label);
                    return IcecatLiveLookupResult.notFound();
                }
                return IcecatLiveLookupResult.found(item);
            } catch (UnrecognizedPropertyException e) {
                LOGGER.error("Unknown property at {} : {}", url, e.getOriginalMessage());
                return IcecatLiveLookupResult.error(e.getOriginalMessage());
            } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest e) {
                LOGGER.info("{} is not found in Icecat", label);
                return IcecatLiveLookupResult.notFound();
            } catch (HttpClientErrorException.Forbidden e) {
                LOGGER.info("{} is restricted to an upgraded Icecat plan", label);
                return IcecatLiveLookupResult.restricted();
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // Transient (5xx, connect/read timeout) : retry up to maxAttempts, others are terminal.
                if (attempt >= maxAttempts) {
                    LOGGER.error("Icecat live call failed after {} attempt(s) for {}", attempt, label, e);
                    return IcecatLiveLookupResult.error(e.getMessage());
                }
                LOGGER.warn("Icecat live call attempt {}/{} failed for {}, retrying : {}", attempt, maxAttempts, label,
                        e.getMessage());
                sleep(config.getRetryBackoffMs());
            } catch (Exception e) {
                LOGGER.error("Unexpected error in icecat parsing for {}", label, e);
                return IcecatLiveLookupResult.error(e.getMessage());
            }
        }
        return IcecatLiveLookupResult.error("Icecat live call failed after " + maxAttempts + " attempt(s) for " + label);
    }

    private static void sleep(Integer millis) {
        try {
            Thread.sleep(millis == null ? 0 : millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Builds the GTIN-search request URL, substituting the configured {@code Language=} query
     * value with the requested {@link DomainLanguage} instead of the static config default.
     */
    String buildUrl(long gtin, DomainLanguage language) {
        String prefix = config.getIceCatUrlPrefix()
                .replaceFirst("(?i)Language=[^&]*", "Language=" + language.languageTag());
        return prefix + gtin;
    }

    /**
     * Builds the Icecat-id request URL, substituting both the configured {@code Language=} value
     * and the trailing {@code GTIN=} query name with {@code icecat_id=}.
     *
     * <p>Verified against Icecat's published JSON API manual (icecat_id query parameter); not
     * verified against the live API itself, since no by-id fixture is available offline.
     */
    String buildIdUrl(String icecatId, DomainLanguage language) {
        String prefix = config.getIceCatUrlPrefix()
                .replaceFirst("(?i)Language=[^&]*", "Language=" + language.languageTag())
                .replaceFirst("(?i)GTIN=$", "icecat_id=");
        return prefix + icecatId;
    }
}

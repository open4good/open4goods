package org.open4goods.icecat.services;

import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatLiveApiResponse;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.model.localization.DomainLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
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
        this(config, RestClient.create(), new ObjectMapper());
    }

    IcecatLiveClient(IcecatCompletionConfig config, RestClient restClient, ObjectMapper objectMapper) {
        this.config = config;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches and parses a single product from the Icecat live API.
     *
     * @param gtin     the product's GTIN
     * @param language the domain language to request localized content in
     * @return the neutral lookup outcome; never throws for expected HTTP/parsing failures
     */
    public IcecatLiveLookupResult fetchProduct(long gtin, DomainLanguage language) {
        String url = buildUrl(gtin, language);
        LOGGER.info("Loading icecat data {}", url);
        try {
            String content = restClient.get().uri(url).retrieve().body(String.class);
            IceDataItem item = objectMapper.readValue(content, IcecatLiveApiResponse.class).data;
            if (item == null || item.generalInfo == null) {
                LOGGER.warn("Icecat response for gtin {} does not contain product data", gtin);
                return IcecatLiveLookupResult.notFound();
            }
            return IcecatLiveLookupResult.found(item);
        } catch (UnrecognizedPropertyException e) {
            LOGGER.error("Unknown property at {} : {}", url, e.getOriginalMessage());
            return IcecatLiveLookupResult.error(e.getOriginalMessage());
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest e) {
            LOGGER.info("Gtin {} is not found in Icecat", gtin);
            return IcecatLiveLookupResult.notFound();
        } catch (HttpClientErrorException.Forbidden e) {
            LOGGER.info("Gtin {} is restricted to an upgraded Icecat plan", gtin);
            return IcecatLiveLookupResult.restricted();
        } catch (Exception e) {
            LOGGER.error("Unexpected error in icecat parsing for gtin {}", gtin, e);
            return IcecatLiveLookupResult.error(e.getMessage());
        }
    }

    /**
     * Builds the request URL, substituting the configured {@code Language=} query value with
     * the requested {@link DomainLanguage} instead of the static config default.
     */
    String buildUrl(long gtin, DomainLanguage language) {
        String prefix = config.getIceCatUrlPrefix()
                .replaceFirst("(?i)Language=[^&]*", "Language=" + language.languageTag());
        return prefix + gtin;
    }
}

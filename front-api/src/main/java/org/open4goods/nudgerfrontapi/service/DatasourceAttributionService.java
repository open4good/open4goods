package org.open4goods.nudgerfrontapi.service;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationPartnersProperties;
import org.open4goods.nudgerfrontapi.dto.product.ProductSourceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Describes the sources of a product: portal URL, licence and attribution text (GOU-2302).
 * <p>
 * The licence fields are stated in the datasource configurations ({@code DataSourceProperties}, the
 * {@code datasources/*.yml} files of open4goods-config), which only the api loads. This service reads them from the
 * api ({@code GET /datasources}, with the base URL and API key of {@code front.affiliation-partners}), keeps the five
 * fields it needs and refreshes them hourly. On failure the previous snapshot is kept: a product page never waits for
 * the api, it only lacks the licences until the next successful refresh.
 * </p>
 * <p>
 * Merchant feeds have no datasource file: their portal URL comes from the affiliation partners
 * ({@link AffiliationPartnerService}). Licence fields are never guessed: a source that does not state them gets
 * {@code null}. Only {@code http(s)} URLs are exposed, since partner URLs come from the affiliation networks.
 * </p>
 */
@Service
public class DatasourceAttributionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceAttributionService.class);

    private static final ParameterizedTypeReference<Map<String, DatasourceConfig>> CONFIGS_TYPE =
            new ParameterizedTypeReference<>() { };

    private static final Comparator<String> BY_NAME =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private final RestClient restClient;
    private final AffiliationPartnerService affiliationPartnerService;
    /** Datasource configurations by matching key ({@link #key(String)}), never partially loaded. */
    private final AtomicReference<Map<String, DatasourceConfig>> configs = new AtomicReference<>(Map.of());

    /**
     * Builds the service against the api configured for the affiliation partners.
     *
     * @param restClientBuilder         Spring's RestClient builder
     * @param partnersProperties        api base URL and API key
     * @param affiliationPartnerService source of the merchant portal URLs
     */
    @Autowired
    public DatasourceAttributionService(RestClient.Builder restClientBuilder,
            AffiliationPartnersProperties partnersProperties,
            AffiliationPartnerService affiliationPartnerService) {
        this(restClientBuilder.baseUrl(partnersProperties.getApiBaseUrl())
                        .defaultHeader(UrlConstants.APIKEY_PARAMETER, partnersProperties.getApiKey())
                        .build(),
                affiliationPartnerService);
    }

    DatasourceAttributionService(RestClient restClient, AffiliationPartnerService affiliationPartnerService) {
        this.restClient = restClient;
        this.affiliationPartnerService = affiliationPartnerService;
    }

    /**
     * Reads the datasource configurations from the api and replaces the snapshot. The api keys them by file name
     * ({@code icecat.biz.yml}); they are re-keyed by their {@code name}, the key of {@code Product.datasourceCodes}
     * and of the attribute sourcing. On failure the previous snapshot is kept; the response body is never logged,
     * since it holds the full configurations.
     */
    @Scheduled(fixedDelayString = "PT1H")
    public void refresh() {
        try {
            Map<String, DatasourceConfig> fetched = restClient.get()
                    .uri(UrlConstants.MASTER_API_DATASOURCES_CONFIG)
                    .retrieve()
                    .body(CONFIGS_TYPE);
            if (fetched == null) {
                LOGGER.warn("Empty datasource configurations from the api, keeping {} cached",
                        configs.get().size());
                return;
            }
            Map<String, DatasourceConfig> byName = new HashMap<>();
            fetched.values().stream()
                    .filter(Objects::nonNull)
                    .filter(config -> StringUtils.hasText(config.name()))
                    .map(DatasourceConfig::normalized)
                    .forEach(config -> byName.putIfAbsent(key(config.name()), config));
            configs.set(Map.copyOf(byName));
            LOGGER.info("Loaded {} datasource configurations from the api, {} stating a licence", byName.size(),
                    byName.values().stream().filter(config -> config.license() != null).count());
        } catch (Exception e) {
            String cause = e instanceof RestClientResponseException response
                    ? "HTTP " + response.getStatusCode().value()
                    : e.getClass().getSimpleName();
            LOGGER.warn("Cannot refresh the datasource attributions, keeping {} cached: {}", configs.get().size(),
                    cause);
        }
    }

    /**
     * Describes the given sources, sorted by name (case insensitive).
     *
     * @param datasourceNames the source names, as in {@code Product.datasourceCodes}; may be null
     * @return one description per distinct non-blank name; the licence fields are null when not stated
     */
    public List<ProductSourceDto> describe(Collection<String> datasourceNames) {
        if (datasourceNames == null || datasourceNames.isEmpty()) {
            return List.of();
        }
        Map<String, DatasourceConfig> current = configs.get();
        Map<String, String> partnerPortals = partnerPortalsByName(affiliationPartnerService.getPartners());
        return datasourceNames.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(BY_NAME)
                .map(name -> describe(name, current.get(key(name)), partnerPortals.get(key(name))))
                .toList();
    }

    /**
     * @param name          source name
     * @param config        its datasource configuration, or null when the api does not know it
     * @param partnerPortal portal URL of the affiliation partner of the same name, or null
     * @return the description, the configured portal URL winning over the partner one
     */
    private static ProductSourceDto describe(String name, DatasourceConfig config, String partnerPortal) {
        if (config == null) {
            return new ProductSourceDto(name, partnerPortal, null, null, null);
        }
        String portalUrl = config.portalUrl() != null ? config.portalUrl() : partnerPortal;
        return new ProductSourceDto(name, portalUrl, config.license(), config.licenseUrl(), config.attribution());
    }

    /**
     * @param partners affiliation partners, may be null
     * @return the http(s) portal URL of each partner by matching key
     */
    private static Map<String, String> partnerPortalsByName(Collection<AffiliationPartner> partners) {
        if (partners == null) {
            return Map.of();
        }
        return partners.stream()
                .filter(partner -> partner != null && StringUtils.hasText(partner.getName()))
                .filter(partner -> httpUrl(partner.getPortalUrl()) != null)
                .collect(Collectors.toMap(partner -> key(partner.getName()),
                        partner -> httpUrl(partner.getPortalUrl()), (first, second) -> first));
    }

    /**
     * @param value a text from the configuration
     * @return the trimmed text, or null when blank
     */
    private static String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * @param value a URL from the configuration or an affiliation network
     * @return the trimmed URL when it is an absolute http(s) URL, null otherwise (blank, relative,
     *         {@code javascript:}...)
     */
    static String httpUrl(String value) {
        String url = text(value);
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            return http && StringUtils.hasText(uri.getHost()) ? url : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @param datasourceName a source name
     * @return the key matching the names of the configurations, the partners and the products
     */
    private static String key(String datasourceName) {
        return datasourceName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * The fields of a datasource configuration ({@code DataSourceProperties}) this service needs; the others are
     * ignored.
     *
     * @param name        datasource name
     * @param portalUrl   portal URL
     * @param license     licence name
     * @param licenseUrl  URL of the licence text
     * @param attribution text crediting the source
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DatasourceConfig(String name, String portalUrl, String license, String licenseUrl, String attribution) {

        /**
         * @return the same configuration with blank texts as null and non http(s) URLs dropped
         */
        DatasourceConfig normalized() {
            return new DatasourceConfig(text(name), httpUrl(portalUrl), text(license), httpUrl(licenseUrl),
                    text(attribution));
        }
    }
}

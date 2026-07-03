package org.open4goods.nudgerfrontapi.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service responsible for resolving public product URLs.
 */
@Service
public class ProductUrlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductUrlService.class);
    private static final String DEFAULT_LANGUAGE_KEY = "default";

    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsConfigService;
    private final CategoryMappingService categoryMappingService;
    private final GoogleIndexationProperties indexationProperties;

    /**
     * Create the product URL service.
     *
     * @param productRepository product repository
     * @param verticalsConfigService vertical configuration service
     * @param categoryMappingService category mapping service
     * @param indexationProperties indexation properties
     */
    public ProductUrlService(ProductRepository productRepository,
            VerticalsConfigService verticalsConfigService,
            CategoryMappingService categoryMappingService,
            GoogleIndexationProperties indexationProperties) {
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
        this.categoryMappingService = categoryMappingService;
        this.indexationProperties = indexationProperties;
    }

    /**
     * Resolve the canonical product URL for a given GTIN.
     *
     * @param gtin product identifier
     * @param domainLanguage requested domain language
     * @return resolved URL or {@code null} when unavailable
     */
    public String resolveProductUrl(long gtin, DomainLanguage domainLanguage) {
        if (!StringUtils.hasText(indexationProperties.getSiteBaseUrl())) {
            LOGGER.warn("Google indexation site base URL is not configured.");
            return null;
        }
        try {
            Product product = productRepository.getByIdWithoutEmbedding(gtin);
            String slug = resolveLocalisedString(product.getNames() != null ? product.getNames().getUrl() : null, domainLanguage);
            String verticalHomeUrl = resolveVerticalHomeUrl(product.getVertical(), domainLanguage);
            String path = buildPath(gtin, slug, verticalHomeUrl);
            return normalizeBaseUrl(indexationProperties.getSiteBaseUrl()) + path;
        } catch (Exception exception) {
            LOGGER.warn("Unable to resolve product URL for GTIN {}: {}", gtin, exception.getMessage());
            return null;
        }
    }

    /**
     * Resolve the vertical home URL using localisation rules.
     *
     * @param verticalId vertical identifier
     * @param domainLanguage domain language
     * @return resolved vertical home URL or {@code null}
     */
    private String resolveVerticalHomeUrl(String verticalId, DomainLanguage domainLanguage) {
        if (!StringUtils.hasText(verticalId)) {
            return null;
        }
        VerticalConfig config = verticalsConfigService.getConfigById(verticalId);
        if (config == null) {
            config = verticalsConfigService.getConfigByIdOrDefault(verticalId);
        }
        VerticalConfigDto dto = categoryMappingService.toVerticalConfigDto(config, domainLanguage);
        if (dto != null && StringUtils.hasText(dto.verticalHomeUrl())) {
            return dto.verticalHomeUrl();
        }
        for (String languageKey : candidateLanguageKeys(domainLanguage)) {
            ProductI18nElements i18n = config.i18n(languageKey);
            if (i18n != null && StringUtils.hasText(i18n.getVerticalHomeUrl())) {
                return i18n.getVerticalHomeUrl();
            }
        }
        return null;
    }

    /**
     * Build candidate language keys for vertical URL fallback lookup.
     *
     * @param domainLanguage requested domain language
     * @return ordered language keys
     */
    private Set<String> candidateLanguageKeys(DomainLanguage domainLanguage) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (domainLanguage != null) {
            String iso = domainLanguage.name();
            String tag = domainLanguage.languageTag();
            String normalizedTag = tag.replace('_', '-');
            keys.add(iso);
            keys.add(iso.toLowerCase(Locale.ROOT));
            keys.add(iso.toUpperCase(Locale.ROOT));
            keys.add(tag);
            keys.add(tag.toLowerCase(Locale.ROOT));
            keys.add(normalizedTag);
            keys.add(normalizedTag.toLowerCase(Locale.ROOT));
            if (normalizedTag.contains("-")) {
                String base = normalizedTag.substring(0, normalizedTag.indexOf('-'));
                keys.add(base);
                keys.add(base.toLowerCase(Locale.ROOT));
            }
        }
        keys.add(DEFAULT_LANGUAGE_KEY);
        return keys;
    }

    /**
     * Resolve a string from a localisable structure using the requested language.
     *
     * @param localisable localised values container
     * @param domainLanguage requested language
     * @return resolved value or {@code null}
     */
    private String resolveLocalisedString(Localisable<String, String> localisable, DomainLanguage domainLanguage) {
        if (localisable == null) {
            return null;
        }
        return localisable.i18n(domainLanguage != null ? domainLanguage.languageTag() : null);
    }

    /**
     * Build the product path from slug and vertical data.
     *
     * @param gtin product identifier
     * @param slug localised slug
     * @param verticalHomeUrl vertical home URL
     * @return path starting with a slash
     */
    private String buildPath(long gtin, String slug, String verticalHomeUrl) {
        if (StringUtils.hasText(slug) && StringUtils.hasText(verticalHomeUrl)) {
            return "/" + trimSlashes(verticalHomeUrl) + "/" + trimSlashes(slug);
        }
        if (StringUtils.hasText(slug)) {
            return "/" + trimSlashes(slug);
        }
        return "/products/" + gtin;
    }

    /**
     * Normalize the base URL by removing any trailing slash.
     *
     * @param baseUrl configured base URL
     * @return normalized base URL
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    /**
     * Trim leading and trailing slashes from a path fragment.
     *
     * @param value path fragment
     * @return trimmed fragment
     */
    private String trimSlashes(String value) {
        String trimmed = value;
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

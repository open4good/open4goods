package org.open4goods.nudgerfrontapi.service;

import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCanonicalUrl;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service responsible for resolving public product URLs.
 *
 * <p>The path follows {@link ProductCanonicalUrl}, the rule shared with the
 * product {@code fullSlug} and the product sitemap.</p>
 */
@Service
public class ProductUrlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductUrlService.class);

    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsConfigService;
    private final GoogleIndexationProperties indexationProperties;

    /**
     * Create the product URL service.
     *
     * @param productRepository product repository
     * @param verticalsConfigService vertical configuration service
     * @param indexationProperties indexation properties
     */
    public ProductUrlService(ProductRepository productRepository,
            VerticalsConfigService verticalsConfigService,
            GoogleIndexationProperties indexationProperties) {
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
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
            String path = ProductCanonicalUrl.fullSlug(product, resolveVerticalConfig(product.getVertical()),
                    domainLanguage);
            if (path == null) {
                path = "/products/" + gtin;
            }
            return normalizeBaseUrl(indexationProperties.getSiteBaseUrl()) + path;
        } catch (Exception exception) {
            LOGGER.warn("Unable to resolve product URL for GTIN {}: {}", gtin, exception.getMessage());
            return null;
        }
    }

    /**
     * Resolve the vertical configuration with fallback to the default
     * configuration, as the product mapping does.
     *
     * @param verticalId vertical identifier
     * @return vertical configuration or {@code null}
     */
    private VerticalConfig resolveVerticalConfig(String verticalId) {
        if (!StringUtils.hasText(verticalId)) {
            return null;
        }
        VerticalConfig config = verticalsConfigService.getConfigById(verticalId);
        if (config == null) {
            config = verticalsConfigService.getConfigByIdOrDefault(verticalId);
        }
        return config;
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
}

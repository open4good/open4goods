package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.stats.ActiveMerchantDomainsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalStatsDto;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.model.stats.ActiveMerchantDomains;
import org.open4goods.nudgerfrontapi.model.stats.AffiliationPartnersStats;
import org.open4goods.services.opendata.service.OpenDataService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectReader;

/**
 * Service exposing aggregated statistics for the frontend API.
 * <p>
 * Statistics are computed from the vertical YAML files shipped with the distribution. The service keeps
 * the resource handling and defensive checks centralised so controllers simply forward the domain language.
 * </p>
 */
@Service
public class StatsService {

    private static final String CLASSPATH_VERTICALS = "classpath:/verticals/*.yml";
    private static final String DEFAULT_CONFIG_RESOURCE = "classpath:/verticals/_default.yml";
    private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";

    private final SerialisationService serialisationService;
    private final ResourcePatternResolver resourceResolver;
    private final AffiliationPartnerService affiliationPartnerService;
    private final OpenDataService openDataService;
    private final ProductRepository productRepository;
    private final MerchantDomainsService merchantDomainsService;

    public StatsService(SerialisationService serialisationService,
                        ResourcePatternResolver resourceResolver,
                        AffiliationPartnerService affiliationPartnerService,
                        OpenDataService openDataService,
                        ProductRepository productRepository,
                        MerchantDomainsService merchantDomainsService) {
        this.serialisationService = serialisationService;
        this.resourceResolver = resourceResolver;
        this.affiliationPartnerService = affiliationPartnerService;
        this.openDataService = openDataService;
        this.productRepository = productRepository;
        this.merchantDomainsService = merchantDomainsService;
    }

    /**
     * Compute statistics about categories mappings.
     *
     * @param domainLanguage currently unused but retained for future localisation of statistics labels
     * @return DTO describing the category statistics used by the frontend including affiliation partners,
     * the distinct merchant domains with an active offer, OpenData counts, rated product counts, and per-category
     * product counts for recent products with offers.
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public CategoriesStatsDto categories(DomainLanguage domainLanguage) {
        List<VerticalConfig> enabledVerticals = loadEnabledVerticals();
        AffiliationPartnersStats partnersStats = computeAffiliationPartnersStats();
        long gtinItemsCount = openDataService.totalItemsGTIN();
        long isbnItemsCount = openDataService.totalItemsISBN();
        long impactScoreProductsCount = safeCount(productRepository.countMainIndexHavingImpactScore());
        long productsWithoutVerticalCount = safeCount(productRepository.countMainIndexWithoutVertical());
        long totalProductsCount = safeCount(productRepository.countMainIndex());
        long excludedProductsCount = safeCount(productRepository.countMainIndexExcluded());
        long ratedProductsCount = safeCount(productRepository.countMainIndexValidAndRated());

        Map<String, Long> productsCountByCategory = new LinkedHashMap<>();
        Map<String, VerticalStatsDto> detailedStats = new LinkedHashMap<>();
        long productsCountSum = 0L;
        for (VerticalConfig vertical : enabledVerticals) {
            String verticalId = vertical.getId();
            if (verticalId == null || verticalId.isBlank()) {
                continue;
            }

            long safeCount = safeCount(productRepository.countMainIndexHavingVertical(verticalId));
            productsCountByCategory.put(verticalId, safeCount);
            productsCountSum += safeCount;

            long vTotal = safeCount(productRepository.countMainIndexTotal(verticalId));
            long vExcluded = safeCount(productRepository.countMainIndexExcluded(verticalId));
            long vRated = safeCount(productRepository.countMainIndexValidAndRated(verticalId));

            detailedStats.put(verticalId, new VerticalStatsDto(vTotal, vExcluded, safeCount, vRated));
        }

        return new CategoriesStatsDto(
                Math.toIntExact(enabledVerticals.size()),
                partnersStats.count(),
                computeActiveMerchantDomains(),
                gtinItemsCount,
                isbnItemsCount,
                impactScoreProductsCount,
                productsWithoutVerticalCount,
                productsCountByCategory,
                productsCountSum,
                totalProductsCount,
                excludedProductsCount,
                ratedProductsCount,
                detailedStats
        );
    }

    /**
     * Compute statistics about affiliation partners so the homepage can display partner counts alongside categories stats.
     *
     * @return immutable stats wrapper describing the partner catalogue
     */
    private AffiliationPartnersStats computeAffiliationPartnersStats() {
        List<AffiliationPartner> partners = affiliationPartnerService.getPartners();
        int partnersCount = partners == null ? 0 : partners.size();

        return new AffiliationPartnersStats(partnersCount);
    }

    /**
     * Read the distinct merchant domains count precomputed by {@link MerchantDomainsService}, next to the partners
     * count: unlike it, affiliation networks, merchants without an active offer and duplicate domains are left out.
     *
     * @return the dated count, or null when it has never been computed
     */
    private ActiveMerchantDomainsDto computeActiveMerchantDomains() {
        ActiveMerchantDomains merchants = merchantDomainsService.current();
        return merchants == null ? null
                : new ActiveMerchantDomainsDto(merchants.count(), merchants.computedAt(), merchants.franceOnly());
    }

    /**
     * Load the default vertical configuration which acts as the base for every other vertical.
     *
     * @return parsed {@link VerticalConfig}
     */
    private VerticalConfig loadDefaultConfig() {
        Resource resource = resourceResolver.getResource(DEFAULT_CONFIG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return serialisationService.fromYaml(inputStream, VerticalConfig.class);
        } catch (IOException | SerialisationException e) {
            throw new IllegalStateException("Cannot load default vertical configuration", e);
        }
    }

    /**
     * List all vertical configuration resources available on the classpath.
     *
     * @return resources matching the vertical glob pattern
     */
    private Resource[] loadVerticalResources() {
        try {
            return resourceResolver.getResources(CLASSPATH_VERTICALS);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list vertical configuration files", e);
        }
    }

    /**
     * Load a single vertical configuration by applying overrides on top of the default configuration.
     *
     * @param resource      YAML resource describing a vertical
     * @param defaultConfig base configuration used as a template
     * @return fully merged configuration
     */
    private VerticalConfig loadVerticalConfig(Resource resource, VerticalConfig defaultConfig) {
        try (InputStream inputStream = resource.getInputStream()) {
            VerticalConfig base = cloneDefault(defaultConfig);
            ObjectReader reader = serialisationService.getYamlMapper().readerForUpdating(base);
            return reader.readValue(inputStream);
        } catch (IOException | SerialisationException e) {
            throw new IllegalStateException("Cannot load vertical configuration " + resource.getFilename(), e);
        }
    }

    /**
     * Load enabled vertical configurations from the classpath.
     *
     * @return list of enabled {@link VerticalConfig}
     */
    private List<VerticalConfig> loadEnabledVerticals() {
        VerticalConfig defaultConfig = loadDefaultConfig();
        Resource[] resources = loadVerticalResources();

        return Arrays.stream(resources)
                .filter(resource -> !Objects.equals(resource.getFilename(), DEFAULT_CONFIG_FILENAME))
                // Copy the default config before merging custom values to keep defaults intact.
                .map(resource -> loadVerticalConfig(resource, defaultConfig))
                .filter(Objects::nonNull)
                .filter(VerticalConfig::isEnabled)
                .toList();
    }

    /**
     * Protect against null counts returned by the repository.
     *
     * @param count repository count result
     * @return non-null count
     */
    private long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    /**
     * Create a deep copy of the default configuration so per-vertical overrides do not mutate the shared instance.
     *
     * @param defaultConfig reference configuration loaded from {@code _default.yml}
     * @return cloned configuration
     * @throws SerialisationException when the YAML serialisation round trip fails
     */
    private VerticalConfig cloneDefault(VerticalConfig defaultConfig) throws SerialisationException {
        String yaml = serialisationService.toYaml(defaultConfig);
        return serialisationService.fromYaml(yaml, VerticalConfig.class);
    }

}

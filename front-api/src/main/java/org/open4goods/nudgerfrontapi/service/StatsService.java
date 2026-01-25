package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesScoreStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesScoresStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoryScoreCardinalitiesDto;
import org.open4goods.nudgerfrontapi.dto.stats.ScoreCardinalityDto;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalStatsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.model.stats.AffiliationPartnersStats;
import org.open4goods.services.opendata.service.OpenDataService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectReader;

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
    private final ProductMappingService productMappingService;

    public StatsService(SerialisationService serialisationService,
                        ResourcePatternResolver resourceResolver,
                        AffiliationPartnerService affiliationPartnerService,
                        OpenDataService openDataService,
                        ProductRepository productRepository,
                        ProductMappingService productMappingService) {
        this.serialisationService = serialisationService;
        this.resourceResolver = resourceResolver;
        this.affiliationPartnerService = affiliationPartnerService;
        this.openDataService = openDataService;
        this.productRepository = productRepository;
        this.productMappingService = productMappingService;
    }

    /**
     * Compute statistics about categories mappings.
     *
     * @param domainLanguage currently unused but retained for future localisation of statistics labels
     * @return DTO describing the category statistics used by the frontend including affiliation partners,
     * OpenData counts, rated/reviewed product counts, and per-category product counts for recent products with offers.
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
        long reviewedProductsCount = safeCount(productRepository.countMainIndexValidAndReviewed(domainLanguage.languageTag()));
        String reviewLocale = domainLanguage != null ? domainLanguage.languageTag() : null;

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
            long vReviewed = safeCount(productRepository.countMainIndexValidAndReviewed(verticalId, domainLanguage.languageTag()));

            detailedStats.put(verticalId, new VerticalStatsDto(vTotal, vExcluded, safeCount, vRated, vReviewed));
        }

        return new CategoriesStatsDto(
                Math.toIntExact(enabledVerticals.size()),
                partnersStats.count(),
                gtinItemsCount,
                isbnItemsCount,
                impactScoreProductsCount,
                productsWithoutVerticalCount,
                productsCountByCategory,
                productsCountSum,
                totalProductsCount,
                excludedProductsCount,
                ratedProductsCount,
                reviewedProductsCount,
                detailedStats
        );
    }

    /**
     * Compute per-category score cardinalities for all available impact score criteria.
     *
     * @param domainLanguage currently unused but retained for future localisation of statistics labels
     * @return DTO containing per-category and per-score cardinalities for absolute and relative scores
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public CategoriesScoresStatsDto categoryScores(DomainLanguage domainLanguage) {
        List<VerticalConfig> enabledVerticals = loadEnabledVerticals();
        Map<String, Map<String, CategoryScoreCardinalitiesDto>> scoresByCategory = new LinkedHashMap<>();

        for (VerticalConfig vertical : enabledVerticals) {
            String verticalId = vertical.getId();
            if (verticalId == null || verticalId.isBlank()) {
                continue;
            }

            Map<String, CategoryScoreCardinalitiesDto> scores = new LinkedHashMap<>();
            if (vertical.getAvailableImpactScoreCriterias() != null) {
                for (String scoreName : vertical.getAvailableImpactScoreCriterias()) {
                    if (scoreName == null || scoreName.isBlank()) {
                        continue;
                    }
                    Cardinality absolute = productRepository.scoreAbsoluteCardinality(scoreName, verticalId);
                    Cardinality relativ = productRepository.scoreRelativCardinality(scoreName, verticalId);
                    scores.put(scoreName, new CategoryScoreCardinalitiesDto(
                            mapScoreCardinality(absolute),
                            mapScoreCardinality(relativ)
                    ));
                }
            }
            scoresByCategory.put(verticalId, scores);
        }

        return new CategoriesScoresStatsDto(scoresByCategory);
    }

    /**
     * Compute per-category score cardinalities for a single impact score criterion.
     *
     * @param domainLanguage currently unused but retained for future localisation of statistics labels
     * @param scoreName      score key to compute for each category
     * @return DTO containing per-category cardinalities for absolute and relative scores
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public CategoriesScoreStatsDto categoryScore(DomainLanguage domainLanguage, String scoreName) {
        List<VerticalConfig> enabledVerticals = loadEnabledVerticals();
        Map<String, CategoryScoreCardinalitiesDto> scoresByCategory = new LinkedHashMap<>();

        if (scoreName == null || scoreName.isBlank()) {
            return new CategoriesScoreStatsDto(scoreName, scoresByCategory);
        }

        for (VerticalConfig vertical : enabledVerticals) {
            String verticalId = vertical.getId();
            if (verticalId == null || verticalId.isBlank()) {
                continue;
            }

            if (vertical.getAvailableImpactScoreCriterias() == null
                    || !vertical.getAvailableImpactScoreCriterias().contains(scoreName)) {
                continue;
            }

            Cardinality absolute = productRepository.scoreAbsoluteCardinality(scoreName, verticalId);
            Cardinality relativ = productRepository.scoreRelativCardinality(scoreName, verticalId);
            scoresByCategory.put(verticalId, new CategoryScoreCardinalitiesDto(
                    mapScoreCardinality(absolute),
                    mapScoreCardinality(relativ)
            ));
        }

        return new CategoriesScoreStatsDto(scoreName, scoresByCategory);
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
     * Map a {@link Cardinality} domain object to the frontend DTO.
     *
     * @param cardinality cardinality statistics to map
     * @return DTO with standard deviation computed as σ
     */
    private ScoreCardinalityDto mapScoreCardinality(Cardinality cardinality) {
        if (cardinality == null) {
            return null;
        }
        return new ScoreCardinalityDto(
                cardinality.getMin(),
                cardinality.getMax(),
                cardinality.getAvg(),
                cardinality.getCount(),
                cardinality.getSum(),
                cardinality.getStdDev()
        );
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

    /**
     * Get a list of random products.
     *
     * @param num            number of products to return
     * @param minOffersCount minimum number of offers
     * @param verticalId     optional vertical filter
     * @param domainLanguage language for localization
     * @return list of random products
     */
    public List<ProductDto> random(int num, int minOffersCount, String verticalId, DomainLanguage domainLanguage) {
        return productRepository.getRandomProducts(num, minOffersCount, verticalId).stream()
                .map(product -> productMappingService.mapProduct(product, null, null, domainLanguage, false))
                .toList();
    }
}

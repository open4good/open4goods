package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreCriteria;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.blog.BlogPostDto;
import org.open4goods.nudgerfrontapi.dto.category.CategoryBreadcrumbItemDto;
import org.open4goods.nudgerfrontapi.dto.category.AttributeConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.AttributesConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.CategoryNavigationDto;
import org.open4goods.nudgerfrontapi.dto.category.FeatureGroupDto;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategoryDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreCriteriaDto;
import org.open4goods.nudgerfrontapi.dto.category.SiteNamingDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalSubsetDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Mapping utilities converting {@link VerticalConfig} domain objects into transport DTOs.
 * <p>
 * The service centralises every projection required by the category endpoints: localisation of texts,
 * fallback rules for optional data and construction of image URLs relying on {@link ApiProperties}.
 * </p>
 */
@Service
public class CategoryMappingService {

    private static final String IMAGE_PREFIX = "/images/verticals/";
    private static final String CATEGORY_PATH_PREFIX = "/categories/";
    private static final String DEFAULT_LANGUAGE_KEY = "default";

    private final ApiProperties apiProperties;
    private final GoogleTaxonomyService googleTaxonomyService;

    public CategoryMappingService(ApiProperties apiProperties, GoogleTaxonomyService googleTaxonomyService) {
        this.apiProperties = apiProperties;
        this.googleTaxonomyService = googleTaxonomyService;
    }
    /**
     * Convert a {@link VerticalConfig} into its summary DTO representation.
     *
     * @param verticalConfig domain object loaded from the configuration service
     * @param domainLanguage language requested by the caller
     * @return DTO exposing the summary view or {@code null} when the source is {@code null}
     */
    public VerticalConfigDto toVerticalConfigDto(VerticalConfig verticalConfig, DomainLanguage domainLanguage) {
        if (verticalConfig == null) {
            return null;
        }

        ProductI18nElements i18n = localise(verticalConfig.getI18n(), domainLanguage);

        return new VerticalConfigDto(
                verticalConfig.getId(),
                verticalConfig.isEnabled(),
                verticalConfig.isPopular(),
                verticalConfig.getGoogleTaxonomyId(),
                verticalConfig.getIcecatTaxonomyId(),
                verticalConfig.getOrder(),
                mapVerticalImageSmall(verticalConfig),
                mapVerticalImageMedium(verticalConfig),
                mapVerticalImageLarge(verticalConfig),
                i18n == null ? null : i18n.getVerticalHomeTitle(),
                i18n == null ? null : i18n.getVerticalHomeDescription(),
                i18n == null ? null : i18n.getVerticalHomeUrl(),
                mapPopularAttributes(verticalConfig, domainLanguage));
    }

    /**
     * Convert a {@link VerticalConfig} into its detailed DTO representation.
     *
     * @param verticalConfig domain object loaded from the configuration service
     * @param domainLanguage language requested by the caller
     * @param relatedPosts most recent blog posts referencing the vertical identifier
     * @return DTO exposing the full vertical configuration or {@code null} when the source is {@code null}
     */
    public VerticalConfigFullDto toVerticalConfigFullDto(VerticalConfig verticalConfig,
                                                        DomainLanguage domainLanguage,
                                                        List<BlogPostDto> relatedPosts) {
        if (verticalConfig == null) {
            return null;
        }

        ProductI18nElements i18n = localise(verticalConfig.getI18n(), domainLanguage);

        return new VerticalConfigFullDto(
                verticalConfig.getId(),
                verticalConfig.isEnabled(),
                verticalConfig.isPopular(),
                verticalConfig.getGoogleTaxonomyId(),
                verticalConfig.getIcecatTaxonomyId(),
                verticalConfig.getOrder(),
                mapVerticalImageSmall(verticalConfig),
                mapVerticalImageMedium(verticalConfig),
                mapVerticalImageLarge(verticalConfig),
                i18n == null ? null : i18n.getVerticalHomeTitle(),
                i18n == null ? null : i18n.getVerticalHomeDescription(),
                i18n == null ? null : i18n.getVerticalHomeUrl(),
                i18n == null ? null : i18n.getVerticalMetaTitle(),
                i18n == null ? null : i18n.getVerticalMetaDescription(),
                i18n == null ? null : i18n.getVerticalMetaOpenGraphTitle(),
                i18n == null ? null : i18n.getVerticalMetaOpenGraphDescription(),
                mapCategoryBreadcrumb(verticalConfig.getGoogleTaxonomyId(), domainLanguage),
                defaultList(relatedPosts),
                defaultList(i18n == null ? null : i18n.getWikiPages()),
                i18n == null ? null : i18n.getAiConfigs(),
                defaultList(verticalConfig.getEcoFilters()),
                defaultList(verticalConfig.getTechnicalFilters()),
                defaultList(verticalConfig.getGlobalTechnicalFilters()),
                defaultMap(verticalConfig.getMatchingCategories()),
                defaultSet(verticalConfig.getExcludingTokensFromCategoriesMatching()),
                defaultSet(verticalConfig.getGenerationExcludedFromCategoriesMatching()),
                defaultSet(verticalConfig.getGenerationExcludedFromAttributesMatching()),
                defaultSet(verticalConfig.getRequiredAttributes()),
                verticalConfig.isForceNameGeneration(),
                defaultMap(verticalConfig.getBrandsAlias()),
                defaultSet(verticalConfig.getBrandsExclusion()),
                verticalConfig.getResourcesConfig(),
                mapAttributesConfig(verticalConfig.getAttributesConfig(), domainLanguage),
                mapPopularAttributes(verticalConfig, domainLanguage),
                mapImpactScoreCriterias(verticalConfig.getAvailableImpactScoreCriterias(), domainLanguage),
                mapImpactScoreConfig(verticalConfig.getImpactScoreConfig(), domainLanguage),
                mapVerticalSubsets(verticalConfig.getSubsets(), domainLanguage),
                mapVerticalSubset(verticalConfig.getBrandsSubset(), domainLanguage),
                verticalConfig.getBarcodeConfig(),
                verticalConfig.getRecommandationsConfig(),
                verticalConfig.getDescriptionsAggregationConfig(),
                verticalConfig.getScoringAggregationConfig(),
                mapFeatureGroups(verticalConfig.getFeatureGroups(), domainLanguage),
                verticalConfig.getWorseLimit(),
                verticalConfig.getBettersLimit());
    }

    /**
     * Build the navigation DTO describing the taxonomy tree around the provided
     * category. The structure mirrors what the legacy Thymeleaf template expects
     * to render the breadcrumbs, the first level navigation cards and the list of
     * leaf verticals.
     *
     * @param category       taxonomy node selected by the caller
     * @param domainLanguage requested language
     * @param havingVertical replicate the legacy flag filtering children with
     *                       verticals in their hierarchy
     * @param topNewProducts     top ranked new products associated with the category
     * @param topOccasionProducts top ranked occasion products associated with the category
     * @return navigation DTO or {@code null} when the category is {@code null}
     */
    public CategoryNavigationDto toCategoryNavigationDto(ProductCategory category,
                                                         DomainLanguage domainLanguage,
                                                         boolean havingVertical,
                                                         List<ProductDto> topNewProducts,
                                                         List<ProductDto> topOccasionProducts) {
        if (category == null) {
            return null;
        }

        String languageKey = languageKey(domainLanguage);

        GoogleCategoryDto current = toGoogleCategoryDto(category, domainLanguage, 0, havingVertical);

        List<ProductCategory> directChildren = havingVertical
                ? category.children(true)
                : defaultList(category.getChildren());

        List<GoogleCategoryDto> childCategories = directChildren.stream()
                .map(child -> toGoogleCategoryDto(child, domainLanguage, 1, havingVertical))
                .filter(Objects::nonNull)
                .toList();

        LinkedHashSet<Integer> excludedIds = directChildren.stream()
                .map(ProductCategory::getGoogleCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<GoogleCategoryDto> descendantVerticals = category.verticals().stream()
                .filter(node -> node.getGoogleCategoryId() != null)
                .filter(node -> !excludedIds.contains(node.getGoogleCategoryId()))
                .map(node -> toGoogleCategoryDto(node, domainLanguage, 0, havingVertical))
                .filter(Objects::nonNull)
                .toList();

        List<GoogleCategoryDto> popularCategories = category.verticals().stream()
                .filter(node -> node != null && node.getVertical() != null && node.getVertical().isPopular())
                .filter(node -> !Objects.equals(node.getGoogleCategoryId(), category.getGoogleCategoryId()))
                .map(node -> toGoogleCategoryDto(node, domainLanguage, 0, havingVertical))
                .filter(Objects::nonNull)
                .filter(dto -> dto.googleCategoryId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(GoogleCategoryDto::googleCategoryId,
                                dto -> dto,
                                (left, right) -> left,
                                LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())));

        return new CategoryNavigationDto(
                current,
                mapBreadcrumbs(category, languageKey),
                childCategories,
                descendantVerticals,
                popularCategories,
                defaultList(topNewProducts),
                defaultList(topOccasionProducts));
    }

    /**
     * Map a product category to the DTO structure used by the API. Children are
     * mapped recursively until {@code depth} reaches zero.
     */
    public GoogleCategoryDto toGoogleCategoryDto(ProductCategory category,
                                                 DomainLanguage domainLanguage,
                                                 int depth,
                                                 boolean havingVertical) {
        if (category == null) {
            return null;
        }

        String languageKey = languageKey(domainLanguage);
        String title = category.getGoogleNames() == null ? null : category.getGoogleNames().i18n(languageKey);
        String slug = category.getGoogleCategoryId() != null && category.getGoogleCategoryId() == 0
                ? ""
                : category.getUrls() == null ? null : category.getUrls().i18n(languageKey);

        List<GoogleCategoryDto> childrenDtos = Collections.emptyList();
        if (depth > 0) {
            List<ProductCategory> children = havingVertical
                    ? category.children(true)
                    : defaultList(category.getChildren());
            childrenDtos = children.stream()
                    .map(child -> toGoogleCategoryDto(child, domainLanguage, depth - 1, havingVertical))
                    .filter(Objects::nonNull)
                    .toList();
        }

        return new GoogleCategoryDto(
                category.getGoogleCategoryId(),
                title,
                slug,
                computeCategoryPath(category, languageKey),
                category.isLeaf(),
                category.getVertical() != null,
                category.isHasVerticals(),
                category.getVertical() == null ? null : toVerticalConfigDto(category.getVertical(), domainLanguage),
                childrenDtos);
    }

    /**
     * Build the breadcrumb entries mirroring the legacy Thymeleaf template.
     */
    private List<CategoryBreadcrumbItemDto> mapBreadcrumbs(ProductCategory category, String languageKey) {
        if (category == null) {
            return Collections.emptyList();
        }

        List<CategoryBreadcrumbItemDto> items = new ArrayList<>();
        items.add(new CategoryBreadcrumbItemDto(null, ""));

        for (ProductCategory node : category.hierarchy()) {
            if (node.getGoogleCategoryId() == null || node.getGoogleCategoryId() == 0) {
                continue;
            }
            String title = node.getGoogleNames() == null ? null : node.getGoogleNames().i18n(languageKey);
            items.add(new CategoryBreadcrumbItemDto(title, computeCategoryPath(node, languageKey)));
        }

        return items;
    }

    /**
     * Build the hierarchical path used to address a taxonomy node.
     */
    private String computeCategoryPath(ProductCategory category, String languageKey) {
        if (category == null || category.getGoogleCategoryId() == null || category.getGoogleCategoryId() == 0) {
            return "";
        }

        ArrayDeque<String> segments = new ArrayDeque<>();
        ProductCategory current = category;
        while (current != null && current.getGoogleCategoryId() != null && current.getGoogleCategoryId() != 0) {
            String slug = current.getUrls() == null ? null : current.getUrls().i18n(languageKey);
            if (StringUtils.hasText(slug)) {
                segments.addFirst(slug);
            }
            current = current.getParent();
        }
        return String.join("/", segments);
    }

    private String languageKey(DomainLanguage domainLanguage) {
        return domainLanguage == null ? DEFAULT_LANGUAGE_KEY : domainLanguage.name();
    }

    /**
     * Map the Google taxonomy hierarchy into breadcrumb items matching the requested language.
     */
    private List<CategoryBreadcrumbItemDto> mapCategoryBreadcrumb(Integer googleTaxonomyId, DomainLanguage domainLanguage) {
        if (googleTaxonomyId == null) {
            return Collections.emptyList();
        }
        ProductCategory category = googleTaxonomyService.byId(googleTaxonomyId);
        if (category == null) {
            return Collections.emptyList();
        }
        return category.hierarchy().stream()
                .map(node -> mapBreadcrumbItem(node, domainLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Convert a single taxonomy node to a breadcrumb DTO.
     */
    private CategoryBreadcrumbItemDto mapBreadcrumbItem(ProductCategory category, DomainLanguage domainLanguage) {
        if (category == null) {
            return null;
        }
        String languageKey = languageKey(domainLanguage);
        String title = category.getGoogleNames() == null ? null : category.getGoogleNames().i18n(languageKey);
        String path = computeCategoryPath(category, languageKey);
        String link = StringUtils.hasText(path) ? CATEGORY_PATH_PREFIX + path : null;

        if (title == null && !StringUtils.hasText(path)) {
            return null;
        }
        return new CategoryBreadcrumbItemDto(title, link);
    }

    /**
     * Map the attributes configuration block for a vertical.
     */
    private AttributesConfigDto mapAttributesConfig(AttributesConfig attributesConfig, DomainLanguage domainLanguage) {
        if (attributesConfig == null) {
            return null;
        }
        List<AttributeConfigDto> configs = defaultList(attributesConfig.getConfigs()).stream()
                .map(attributeConfig -> mapAttributeConfig(attributeConfig, domainLanguage))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new AttributesConfigDto(configs,
                defaultSet(attributesConfig.getFeaturedValues()),
                defaultSet(attributesConfig.getExclusions()));
    }

    /**
     * Map the configured popular attributes to their DTO representation.
     */
    private List<AttributeConfigDto> mapPopularAttributes(VerticalConfig verticalConfig, DomainLanguage domainLanguage) {
        if (verticalConfig == null || verticalConfig.getAttributesConfig() == null) {
            return Collections.emptyList();
        }

        return defaultList(verticalConfig.getPopularAttributes()).stream()
                .map(verticalConfig.getAttributesConfig()::getAttributeConfigByKey)
                .map(attributeConfig -> mapAttributeConfig(attributeConfig, domainLanguage))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Map a single attribute configuration entry.
     */
    private AttributeConfigDto mapAttributeConfig(AttributeConfig attributeConfig, DomainLanguage domainLanguage) {
        if (attributeConfig == null) {
            return null;
        }
        return new AttributeConfigDto(
                attributeConfig.getKey(),
                attributeConfig.getFaIcon(),
                localise(attributeConfig.getUnit(), domainLanguage),
                localise(attributeConfig.getName(), domainLanguage),
                attributeConfig.getFilteringType(),
                defaultSet(attributeConfig.getIcecatFeaturesIds()),
                attributeConfig.isAsScore(),
                attributeConfig.isReverseScore(),
                attributeConfig.getBetterIs(),
                attributeConfig.getAttributeValuesOrdering(),
                attributeConfig.getAttributeValuesReverseOrder(),
                defaultMap(attributeConfig.getSynonyms()),
                attributeConfig.getParser(),
                defaultMap(attributeConfig.getNumericMapping()),
                defaultMap(attributeConfig.getMappings()));
    }

    /**
     * Map impact score criteria definitions keyed by their identifier.
     */
    private Map<String, ImpactScoreCriteriaDto> mapImpactScoreCriterias(Map<String, ImpactScoreCriteria> criterias,
                                                                         DomainLanguage domainLanguage) {
        if (criterias == null || criterias.isEmpty()) {
            return Collections.emptyMap();
        }
        return criterias.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> mapImpactScoreCriteria(entry.getValue(), domainLanguage),
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    /**
     * Map a single impact score criterion.
     */
    private ImpactScoreCriteriaDto mapImpactScoreCriteria(ImpactScoreCriteria criteria, DomainLanguage domainLanguage) {
        if (criteria == null) {
            return null;
        }
        return new ImpactScoreCriteriaDto(
                criteria.getKey(),
                localise(criteria.getTitle(), domainLanguage),
                localise(criteria.getDescription(), domainLanguage));
    }

    /**
     * Map the impact score configuration (weights and prompts).
     */
    private ImpactScoreConfigDto mapImpactScoreConfig(ImpactScoreConfig impactScoreConfig, DomainLanguage domainLanguage) {
        if (impactScoreConfig == null) {
            return null;
        }
        return new ImpactScoreConfigDto(
                defaultMap(impactScoreConfig.getCriteriasPonderation()),
                localise(impactScoreConfig.getTexts(), domainLanguage),
                impactScoreConfig.getYamlPrompt(),
                impactScoreConfig.getAiJsonResponse());
    }


    /**
     * Map configured vertical subsets (curated selections) to DTOs.
     */
    private List<VerticalSubsetDto> mapVerticalSubsets(List<VerticalSubset> subsets, DomainLanguage domainLanguage) {
        return defaultList(subsets).stream()
                .map(subset -> mapVerticalSubset(subset, domainLanguage))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Map an individual vertical subset entry.
     */
    private VerticalSubsetDto mapVerticalSubset(VerticalSubset subset, DomainLanguage domainLanguage) {
        if (subset == null) {
            return null;
        }
        return new VerticalSubsetDto(
                subset.getId(),
                subset.getGroup(),
                defaultList(subset.getCriterias()),
                subset.getImage(),
                localise(subset.getUrl(), domainLanguage),
                localise(subset.getCaption(), domainLanguage),
                localise(subset.getTitle(), domainLanguage),
                localise(subset.getDescription(), domainLanguage));
    }

    /**
     * Map feature group definitions used to organise characteristics in the UI.
     */
    private List<FeatureGroupDto> mapFeatureGroups(List<FeatureGroup> featureGroups, DomainLanguage domainLanguage) {
        return defaultList(featureGroups).stream()
                .map(featureGroup -> mapFeatureGroup(featureGroup, domainLanguage))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Map a single feature group entry.
     */
    private FeatureGroupDto mapFeatureGroup(FeatureGroup featureGroup, DomainLanguage domainLanguage) {
        if (featureGroup == null) {
            return null;
        }
        return new FeatureGroupDto(
                featureGroup.getIcecatCategoryFeatureGroupId(),
                localise(featureGroup.getName(), domainLanguage),
                defaultList(featureGroup.getFeaturesId()));
    }

    /**
     * Defensive helper returning an empty map when the source is {@code null}.
     */
    private <K, V> Map<K, V> defaultMap(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Defensive helper returning an empty list when the source is {@code null}.
     */
    private <T> List<T> defaultList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Defensive helper returning an empty set when the source is {@code null}.
     */
    private <T> Set<T> defaultSet(Set<T> set) {
        return set == null ? Collections.emptySet() : set;
    }

    /**
     * Resolve the best matching value for the requested language.
     */
    private <T> T localise(Map<String, T> values, DomainLanguage domainLanguage) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String key : candidateLanguageKeys(domainLanguage)) {
            if (values.containsKey(key)) {
                T value = values.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return values.values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Build a deterministic list of language keys tested when localising values.
     */
    private Set<String> candidateLanguageKeys(DomainLanguage domainLanguage) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
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

        keys.add(DEFAULT_LANGUAGE_KEY);
        return keys;
    }
    /**
     * Build the URL to the medium sized vertical illustration.
     */
    private String mapVerticalImageMedium(VerticalConfig verticalConfig) {
        if (verticalConfig == null || !StringUtils.hasText(apiProperties.getResourceRootPath())) {
            return null;
        }
        return apiProperties.getResourceRootPath() + IMAGE_PREFIX + verticalConfig.getId() + "-360.webp";
    }

    /**
     * Build the URL to the small sized vertical illustration.
     */
    private String mapVerticalImageSmall(VerticalConfig verticalConfig) {
        if (verticalConfig == null || !StringUtils.hasText(apiProperties.getResourceRootPath())) {
            return null;
        }
        return apiProperties.getResourceRootPath() + IMAGE_PREFIX + verticalConfig.getId() + "-100.webp";
    }

    /**
     * Build the URL to the large vertical illustration used on landing pages.
     */
    private String mapVerticalImageLarge(VerticalConfig verticalConfig) {
        if (verticalConfig == null || !StringUtils.hasText(apiProperties.getResourceRootPath())) {
            return null;
        }
        return apiProperties.getResourceRootPath() + IMAGE_PREFIX + verticalConfig.getId() + ".webp";
    }
}

package org.open4goods.nudgerfrontapi.service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigI18nDto;
import org.springframework.stereotype.Service;

/**
 * Mapping utilities converting {@link VerticalConfig} domain objects into transport DTOs.
 */
@Service
public class CategoryMappingService {

    /**
     * Convert a {@link VerticalConfig} into its summary DTO representation.
     *
     * @param verticalConfig domain object loaded from the configuration service
     * @return DTO exposing the summary view or {@code null} when the source is {@code null}
     */
    public VerticalConfigDto toVerticalConfigDto(VerticalConfig verticalConfig) {
        if (verticalConfig == null) {
            return null;
        }

        return new VerticalConfigDto(
                verticalConfig.getId(),
                verticalConfig.isEnabled(),
                verticalConfig.getGoogleTaxonomyId(),
                verticalConfig.getIcecatTaxonomyId(),
                verticalConfig.getOrder(),
                null, // TODO(front-api): populate thumbnail from media catalog once available.
                null, // TODO(front-api): populate hero image from media catalog once available.
                null, // TODO(front-api): expose singular label when configuration is extended.
                null, // TODO(front-api): expose plural label when configuration is extended.
                mapI18n(verticalConfig.getI18n()));
    }

    /**
     * Convert a {@link VerticalConfig} into its detailed DTO representation.
     *
     * @param verticalConfig domain object loaded from the configuration service
     * @return DTO exposing the full vertical configuration or {@code null} when the source is {@code null}
     */
    public VerticalConfigFullDto toVerticalConfigFullDto(VerticalConfig verticalConfig) {
        if (verticalConfig == null) {
            return null;
        }

        return new VerticalConfigFullDto(
                verticalConfig.getId(),
                verticalConfig.isEnabled(),
                verticalConfig.getGoogleTaxonomyId(),
                verticalConfig.getIcecatTaxonomyId(),
                verticalConfig.getOrder(),
                null, // TODO(front-api): populate thumbnail from media catalog once available.
                null, // TODO(front-api): populate hero image from media catalog once available.
                null, // TODO(front-api): expose singular label when configuration is extended.
                null, // TODO(front-api): expose plural label when configuration is extended.
                mapI18n(verticalConfig.getI18n()),
                defaultMap(verticalConfig.getI18n()),
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
                verticalConfig.getNamings(),
                verticalConfig.getResourcesConfig(),
                verticalConfig.getAttributesConfig(),
                defaultMap(verticalConfig.getAvailableImpactScoreCriterias()),
                verticalConfig.getImpactScoreConfig(),
                defaultList(verticalConfig.getSubsets()),
                verticalConfig.getBrandsSubset(),
                verticalConfig.getBarcodeConfig(),
                verticalConfig.getRecommandationsConfig(),
                verticalConfig.getDescriptionsAggregationConfig(),
                verticalConfig.getScoringAggregationConfig(),
                defaultList(verticalConfig.getFeatureGroups()),
                verticalConfig.getWorseLimit(),
                verticalConfig.getBettersLimit());
    }

    private Map<String, VerticalConfigI18nDto> mapI18n(Map<String, ProductI18nElements> i18n) {
        if (i18n == null || i18n.isEmpty()) {
            return Collections.emptyMap();
        }
        return i18n.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new VerticalConfigI18nDto(
                        entry.getValue().getVerticalHomeTitle(),
                        entry.getValue().getVerticalHomeDescription(),
                        entry.getValue().getVerticalHomeUrl())));
    }

    private <T> Map<String, T> defaultMap(Map<String, T> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    private <T> java.util.List<T> defaultList(java.util.List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private <T> java.util.Set<T> defaultSet(java.util.Set<T> set) {
        return set == null ? Collections.emptySet() : set;
    }
}

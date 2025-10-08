package org.open4goods.nudgerfrontapi.service;

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
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.AttributeConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.AttributesConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.FeatureGroupDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreCriteriaDto;
import org.open4goods.nudgerfrontapi.dto.category.SiteNamingDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalSubsetDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Mapping utilities converting {@link VerticalConfig} domain objects into transport DTOs.
 */
@Service
public class CategoryMappingService {

	private static final String IMAGE_PREFIX ="/images/verticals/";
    private static final String DEFAULT_LANGUAGE_KEY = "default";

    @Autowired ApiProperties apiProperties;
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
                verticalConfig.getGoogleTaxonomyId(),
                verticalConfig.getIcecatTaxonomyId(),
                verticalConfig.getOrder(),
                mapVerticalImageSmall(verticalConfig),
                mapVerticalImageMedium(verticalConfig),
                mapVerticalImageLarge(verticalConfig),
                i18n == null ? null : i18n.getVerticalHomeTitle(),
                i18n == null ? null : i18n.getVerticalHomeDescription(),
                i18n == null ? null : i18n.getVerticalHomeUrl());
    }



	/**
     * Convert a {@link VerticalConfig} into its detailed DTO representation.
     *
     * @param verticalConfig domain object loaded from the configuration service
     * @param domainLanguage language requested by the caller
     * @return DTO exposing the full vertical configuration or {@code null} when the source is {@code null}
     */
    public VerticalConfigFullDto toVerticalConfigFullDto(VerticalConfig verticalConfig, DomainLanguage domainLanguage) {
        if (verticalConfig == null) {
            return null;
        }

        ProductI18nElements i18n = localise(verticalConfig.getI18n(), domainLanguage);

        return new VerticalConfigFullDto(
                verticalConfig.getId(),
                verticalConfig.isEnabled(),
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
                attributeConfig.getAttributeValuesOrdering(),
                attributeConfig.getAttributeValuesReverseOrder(),
                defaultMap(attributeConfig.getSynonyms()),
                attributeConfig.getParser(),
                defaultMap(attributeConfig.getNumericMapping()),
                defaultMap(attributeConfig.getMappings()));
    }

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

    private ImpactScoreCriteriaDto mapImpactScoreCriteria(ImpactScoreCriteria criteria, DomainLanguage domainLanguage) {
        if (criteria == null) {
            return null;
        }
        return new ImpactScoreCriteriaDto(
                criteria.getKey(),
                localise(criteria.getTitle(), domainLanguage),
                localise(criteria.getDescription(), domainLanguage));
    }

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


    private List<VerticalSubsetDto> mapVerticalSubsets(List<VerticalSubset> subsets, DomainLanguage domainLanguage) {
        return defaultList(subsets).stream()
                .map(subset -> mapVerticalSubset(subset, domainLanguage))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

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

    private List<FeatureGroupDto> mapFeatureGroups(List<FeatureGroup> featureGroups, DomainLanguage domainLanguage) {
        return defaultList(featureGroups).stream()
                .map(featureGroup -> mapFeatureGroup(featureGroup, domainLanguage))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private FeatureGroupDto mapFeatureGroup(FeatureGroup featureGroup, DomainLanguage domainLanguage) {
        if (featureGroup == null) {
            return null;
        }
        return new FeatureGroupDto(
                featureGroup.getIcecatCategoryFeatureGroupId(),
                localise(featureGroup.getName(), domainLanguage),
                defaultList(featureGroup.getFeaturesId()));
    }

    private <K, V> Map<K, V> defaultMap(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private <T> Set<T> defaultSet(Set<T> set) {
        return set == null ? Collections.emptySet() : set;
    }

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
	 * Return the medium image size, from derivating ResourceControler (ui/static project) mapping ("/images/verticals/{verticalId}.jpg")
	 * Using webp, live conversion filters will translate.
	 * The suffix "-SIZE" must be allowed in static resource app config (allowedImagesSizeSuffixes)
	 * @param verticalConfig
	 * @return
	 */
    private String mapVerticalImageMedium(VerticalConfig verticalConfig) {
		return apiProperties.getResourceRootPath() + IMAGE_PREFIX + verticalConfig.getId() + "-100.webp";
	}

	/**
	 * Return the medium image size, from derivating ResourceControler (ui/static project) mapping ("/images/verticals/{verticalId}.jpg")
	 * Using webp, live conversion filters will translate
     * The suffix "-SIZE" must be allowed in static resource app config (allowedImagesSizeSuffixes)
	 * @param verticalConfig
	 * @return
	 */

	private String mapVerticalImageSmall(VerticalConfig verticalConfig) {
		return apiProperties.getResourceRootPath() + IMAGE_PREFIX + verticalConfig.getId() + "-360.webp";
	}

	/**
	 * Return the medium image size, from derivating ResourceControler (ui/static project) mapping ("/images/verticals/{verticalId}.jpg")
	 * Using webp, live conversion filters will translate
	 * The suffix "-SIZE" must be allowed in static resource app config (allowedImagesSizeSuffixes)
	 * @param verticalConfig
	 * @return
	 */

	private String mapVerticalImageLarge(VerticalConfig verticalConfig) {
		return apiProperties.getResourceRootPath() + IMAGE_PREFIX + verticalConfig.getId() + ".webp";
	}
}

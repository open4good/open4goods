package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.vertical.AiPromptsConfig;
import org.open4goods.model.vertical.BarcodeAggregationProperties;
import org.open4goods.model.vertical.DescriptionsAggregationConfig;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.RecommandationsConfig;
import org.open4goods.model.vertical.ResourcesAggregationConfig;
import org.open4goods.model.vertical.ScoringAggregationConfig;
import org.open4goods.model.vertical.WikiPageConfig;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detailed representation of a vertical configuration mirroring the YAML structure consumed by the core services.
 */
public record VerticalConfigFullDto(
        @Schema(description = "Unique identifier of the vertical configuration.", example = "tv")
        String id,
        @Schema(description = "Indicates whether the vertical is exposed to end-users.", example = "true")
        boolean enabled,
        @Schema(description = "Google taxonomy identifier associated with this vertical.", example = "404")
        Integer googleTaxonomyId,
        @Schema(description = "Icecat taxonomy identifier associated with this vertical.", example = "1584")
        Integer icecatTaxonomyId,
        @Schema(description = "Display order used to render the category list.", example = "1")
        Integer order,
        @Schema(description = "Thumbnail image representing the vertical. TODO(front-api): populate when media assets are defined.",
                nullable = true)
        String imageThumbnail,
        @Schema(description = "Primary image for the vertical hero section. TODO(front-api): populate when media assets are defined.",
                nullable = true)
        String image,
        @Schema(description = "Localised singular label for the vertical. TODO(front-api): populate when naming strategy is available.",
                nullable = true)
        String singularName,
        @Schema(description = "Localised plural label for the vertical. TODO(front-api): populate when naming strategy is available.",
                nullable = true)
        String pluralName,
        @Schema(description = "Localised home title for the vertical.", example = "Téléviseurs")
        String verticalHomeTitle,
        @Schema(description = "Localised home description for the vertical.", example = "Comparez les téléviseurs responsables")
        String verticalHomeDescription,
        @Schema(description = "Localised home URL slug for the vertical.", example = "televiseurs")
        String verticalHomeUrl,
        @Schema(description = "Localised URL configuration used to generate product pages.")
        PrefixedAttrText url,
        @Schema(description = "Localised H1 title configuration for product pages.")
        PrefixedAttrText h1Title,
        @Schema(description = "Localised product meta title displayed in the HTML head.")
        String productMetaTitle,
        @Schema(description = "Localised product meta description displayed in the HTML head.")
        String productMetaDescription,
        @Schema(description = "Localised Open Graph title for product meta tags.")
        String productMetaOpenGraphTitle,
        @Schema(description = "Localised Open Graph description for product meta tags.")
        String productMetaOpenGraphDescription,
        @Schema(description = "Localised Twitter title for product meta tags.")
        String productMetaTwitterTitle,
        @Schema(description = "Localised Twitter description for product meta tags.")
        String productMetaTwitterDescription,
        @Schema(description = "Localised meta title for the vertical landing page.")
        String verticalMetaTitle,
        @Schema(description = "Localised meta description for the vertical landing page.")
        String verticalMetaDescription,
        @Schema(description = "Localised Open Graph title for the vertical landing page.")
        String verticalMetaOpenGraphTitle,
        @Schema(description = "Localised Open Graph description for the vertical landing page.")
        String verticalMetaOpenGraphDescription,
        @Schema(description = "Localised Twitter title for the vertical landing page.")
        String verticalMetaTwitterTitle,
        @Schema(description = "Localised Twitter description for the vertical landing page.")
        String verticalMetaTwitterDescription,
        @Schema(description = "Localised wiki pages associated with the vertical.")
        List<WikiPageConfig> wikiPages,
        @Schema(description = "Localised AI generation configuration for the vertical.")
        AiPromptsConfig aiConfigs,
        @Schema(description = "Eco filters enabled for this vertical.")
        List<String> ecoFilters,
        @Schema(description = "Technical filters enabled for this vertical.")
        List<String> technicalFilters,
        @Schema(description = "Global technical filters applied across datasources.")
        List<String> globalTechnicalFilters,
        @Schema(description = "Mapping of datasource categories to this vertical.")
        Map<String, Set<String>> matchingCategories,
        @Schema(description = "Tokens that exclude a product from matching this vertical.")
        Set<String> excludingTokensFromCategoriesMatching,
        @Schema(description = "Datasources ignored when computing category matches.")
        Set<String> generationExcludedFromCategoriesMatching,
        @Schema(description = "Attributes ignored when computing attribute suggestions.")
        Set<String> generationExcludedFromAttributesMatching,
        @Schema(description = "Attributes required for the product to belong to this vertical.")
        Set<String> requiredAttributes,
        @Schema(description = "Regenerate URL friendly names even if already generated.")
        boolean forceNameGeneration,
        @Schema(description = "Mappings of brand aliases to canonical brand names.")
        Map<String, String> brandsAlias,
        @Schema(description = "Brands excluded from this vertical.")
        Set<String> brandsExclusion,
        @Schema(description = "Localised site naming configuration inherited by the UI.")
        SiteNamingDto namings,
        @Schema(description = "Configuration for media aggregation.")
        ResourcesAggregationConfig resourcesConfig,
        @Schema(description = "Configuration for attribute aggregation and filters.")
        AttributesConfigDto attributesConfig,
        @Schema(description = "Impact score criteria available for this vertical with localised metadata.")
        Map<String, ImpactScoreCriteriaDto> availableImpactScoreCriterias,
        @Schema(description = "Impact score configuration balancing each criterion, with localised texts.")
        ImpactScoreConfigDto impactScoreConfig,
        @Schema(description = "Custom subsets defined for this vertical with localised labels.")
        List<VerticalSubsetDto> subsets,
        @Schema(description = "Subset dedicated to brand exploration with localised labels.")
        VerticalSubsetDto brandsSubset,
        @Schema(description = "Barcode aggregation configuration.")
        BarcodeAggregationProperties barcodeConfig,
        @Schema(description = "Recommendation engine configuration.")
        RecommandationsConfig recommandationsConfig,
        @Schema(description = "Description aggregation configuration.")
        DescriptionsAggregationConfig descriptionsAggregationConfig,
        @Schema(description = "Scoring aggregation configuration.")
        ScoringAggregationConfig scoringAggregationConfig,
        @Schema(description = "Feature groups ordering attributes for UI rendering with localised labels.")
        List<FeatureGroupDto> featureGroups,
        @Schema(description = "Threshold defining how many low scores are considered worsts.")
        Integer worseLimit,
        @Schema(description = "Threshold defining how many high scores are considered bests.")
        Integer bettersLimit
) {
}

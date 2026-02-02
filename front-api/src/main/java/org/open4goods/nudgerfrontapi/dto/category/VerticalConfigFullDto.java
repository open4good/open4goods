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
import org.open4goods.nudgerfrontapi.dto.blog.BlogPostDto;
import org.open4goods.nudgerfrontapi.dto.category.NudgeToolConfigDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detailed representation of a vertical configuration mirroring the YAML structure consumed by the core services.
 */
public record VerticalConfigFullDto(
        @Schema(description = "Unique identifier of the vertical configuration.", example = "tv")
        String id,
        @Schema(description = "Indicates whether the vertical is exposed to end-users.", example = "true")
        boolean enabled,
        @Schema(description = "Marks the category as popular when true.", example = "true")
        boolean popular,
        @Schema(description = "Google taxonomy identifier associated with this vertical.", example = "404")
        Integer googleTaxonomyId,
        @Schema(description = "Icecat taxonomy identifier associated with this vertical.", example = "1584")
        Integer icecatTaxonomyId,
        @Schema(description = "Display order used to render the category list.", example = "1")
        Integer order,
        @Schema(description = "Average daily usage used for cost estimation, in hours.", example = "4.0")
        Double averageHoursPerDay,
        @Schema(description = "Material Design icon name representing the vertical.", example = "television")
        String mdiIcon,
        @Schema(description = "Thumbnail image representing the vertical.")
        String imageSmall,
        @Schema(description = "Medium image for the vertical ")
        String imageMedium,
        @Schema(description = "Large image for the vertical ")
        String imageLarge,
        @Schema(description = "Localised home title for the vertical.", example = "Téléviseurs")
        String verticalHomeTitle,
        @Schema(description = "Localised home description for the vertical.", example = "Comparez les téléviseurs responsables")
        String verticalHomeDescription,
        @Schema(description = "Localised home URL slug for the vertical.", example = "televiseurs")
        String verticalHomeUrl,
        @Schema(description = "Localised meta title for the vertical landing page.")
        String verticalMetaTitle,
        @Schema(description = "Localised meta description for the vertical landing page.")
        String verticalMetaDescription,
        @Schema(description = "Localised Open Graph title for the vertical landing page.")
        String verticalMetaOpenGraphTitle,
        @Schema(description = "Localised Open Graph description for the vertical landing page.")
        String verticalMetaOpenGraphDescription,
        @Schema(description = "Breadcrumb derived from the Google taxonomy hierarchy for this vertical.")
        List<CategoryBreadcrumbItemDto> breadCrumb,
        @Schema(description = "Most recent blog posts tagged with the vertical identifier.")
        List<BlogPostDto> relatedPosts,
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
        @Schema(description = "Configuration for media aggregation.")
        ResourcesAggregationConfig resourcesConfig,
        @Schema(description = "Configuration for attribute aggregation and filters.")
        AttributesConfigDto attributesConfig,
        @Schema(description = "Popular attributes resolved to their full configuration metadata.")
        List<AttributeConfigDto> popularAttributes,
        @Schema(description = "Identifiers of impact score criteria available for this vertical.")
        List<String> availableImpactScoreCriterias,
        @Schema(description = "Identifiers of composite scores aggregating score attributes for the vertical.")
        Set<String> aggregatedScores,
        @Schema(description = "Impact score configuration balancing each criterion, with localised texts.")
        ImpactScoreConfigDto impactScoreConfig,
        @Schema(description = "Custom subsets defined for this vertical with localised labels.")
        List<VerticalSubsetDto> subsets,
        @Schema(description = "Configuration supporting the guided nudge tool for this vertical.")
        NudgeToolConfigDto nudgeToolConfig,
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
        List<FeatureGroupDto> featureGroups
) {
}

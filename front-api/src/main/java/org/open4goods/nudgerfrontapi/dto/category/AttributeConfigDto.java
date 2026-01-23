package org.open4goods.nudgerfrontapi.dto.category;

import java.util.Map;
import java.util.Set;

import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.vertical.AttributeComparisonRule;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.Order;
import org.open4goods.model.vertical.lifecycle.LifecycleStage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing attribute configuration details with localised fields resolved.
 */
public record AttributeConfigDto(
        @Schema(description = "Unique key identifying the attribute.", example = "WEIGHT")
        String key,
        @Schema(description = "Icon associated with the attribute.", example = "fa-weight-hanging")
        String icon,
        @Schema(description = "Localised unit displayed for this attribute.", example = "kg")
        String unit,
        @Schema(description = "Localised suffix appended to the attribute value for compact rendering.", example = "\"")
        String suffix,
        @Schema(description = "Localised display name for this attribute.", example = "Poids")
        String name,
        @Schema(description = "Type of filtering applied for this attribute.")
        AttributeType filteringType,
        @Schema(description = "Identifiers of the Icecat features mapped to this attribute.")
        Set<String> icecatFeaturesIds,
        @Schema(description = "Indicates whether this attribute is exposed as a score.")
        boolean asScore,
        @Schema(description = "Localised label used when the attribute is rendered as a score.")
        String scoreTitle,
        @Schema(description = "Localised description of what the score represents.")
        String scoreDescription,
        @Schema(description = "Localised explanation of why this score is important.")
        String scoreUtility,
        @Schema(description = "Composite scores in which this attribute participates.")
        Set<String> participateInScores,
        @Schema(description = "Lifecycle stages (ACV) represented by this score.")
        Set<LifecycleStage> participateInACV,
        @Schema(description = "Comparison rule applied for user-facing explanations.")
        AttributeComparisonRule userBetterIs,
        @Schema(description = "Comparison rule applied for impact scoring.")
        AttributeComparisonRule impactBetterIs,
        @Schema(description = "Scoring configuration used for this attribute.")
        ScoreScoringConfigDto scoring,
        @Schema(description = "Ordering applied when displaying attribute values.")
        Order attributeValuesOrdering,
        @Schema(description = "When true, the attribute values ordering is reversed.")
        Boolean attributeValuesReverseOrder,
        @Schema(description = "Attribute synonyms keyed by datasource name.")
        Map<String, Set<String>> synonyms,
        @Schema(description = "Custom parser configuration used to normalize attribute values.")
        AttributeParserConfig parser,
        @Schema(description = "Mapping used to convert textual values to numerics when relevant.")
        Map<String, Double> numericMapping,
        @Schema(description = "Static replacements applied to attribute values.")
        Map<String, String> mappings
) {
}

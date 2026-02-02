package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary representation of a vertical configuration exposed to the frontend.
 */
public record VerticalConfigDto(
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
        @Schema(description = "Popular attributes resolved to their full configuration metadata.")
        List<AttributeConfigDto> popularAttributes,
        @Schema(description = "Identifiers of composite scores aggregating score attributes for the vertical.")
        Set<String> aggregatedScores,
        @Schema(description = "Configuration supporting the guided nudge tool for this vertical.")
        NudgeToolConfigDto nudgeToolConfig
) {
}

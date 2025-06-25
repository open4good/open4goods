package org.open4goods.nudgerfrontapi.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregated response used by the home page.
 */
public record HomeCompositeResponse(
        @Schema(description = "Global statistics")
        StatsDto stats,

        @Schema(description = "List of homepage partners")
        List<PartnerDto> partners) {
}

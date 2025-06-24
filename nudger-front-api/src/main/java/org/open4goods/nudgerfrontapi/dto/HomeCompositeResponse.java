package org.open4goods.nudgerfrontapi.dto;

import java.util.List;

/**
 * Aggregated response used by the home page.
 */
public record HomeCompositeResponse(StatsDto stats, List<PartnerDto> partners) {
}

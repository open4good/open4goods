package org.open4goods.nudgerfrontapi.dto.event;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing commercial event details to the frontend.
 */
@Schema(name = "CommercialEvent", description = "Commercial event highlighted on the eco-nudger calendar.")
public record CommercialEventDto(@Schema(description = "Localised event label.", example = "Black Friday") String label,

		@Schema(description = "Inclusive event start date.", type = "string", format = "date", example = "2025-11-28") LocalDate startDate,

		@Schema(description = "Inclusive event end date.", type = "string", format = "date", example = "2025-11-29") LocalDate endDate

) {
}

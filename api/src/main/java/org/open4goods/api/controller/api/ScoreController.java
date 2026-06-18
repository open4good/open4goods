package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for triggering product scoring runs.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Scoring", description = "Trigger ImpactScore and EcoScore computation runs for one or all verticals.")
public class ScoreController {

    private final VerticalsConfigService verticalConfigService;
    private final AggregationFacadeService aggregationFacadeService;

    public ScoreController(AggregationFacadeService aggregationFacadeService,
                           VerticalsConfigService verticalsConfigService) {
        this.verticalConfigService = verticalsConfigService;
        this.aggregationFacadeService = aggregationFacadeService;
    }

    @PostMapping("/score/{vertical}")
    @Operation(
            summary = "Score all products in a specific vertical",
            description = "Recomputes the ImpactScore and EcoScore for every product that belongs to the given vertical. "
                    + "This is equivalent to running the scoring phase of the batch pipeline but scoped to a single category. "
                    + "See /score for running scoring across all verticals at once.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scoring run completed for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void scoreVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @PathVariable @NotBlank final String vertical)
            throws InvalidParameterException, IOException, InterruptedException {
        aggregationFacadeService.score(verticalConfigService.getConfigById(vertical));
    }

    @PostMapping("/score")
    @Operation(
            summary = "Score all verticals",
            description = "Sanitises all products and then triggers the full batch scoring pipeline that recomputes "
                    + "ImpactScore and EcoScore for every product in every active vertical. "
                    + "Equivalent to the scheduled nightly batch but triggerable on demand.")
    @ApiResponse(responseCode = "200", description = "Full scoring batch completed")
    public void scoreAll() throws InvalidParameterException, IOException, InterruptedException {
        aggregationFacadeService.scoreAll();
    }
}

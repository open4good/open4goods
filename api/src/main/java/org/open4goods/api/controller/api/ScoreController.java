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
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for triggering product scoring runs.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class ScoreController {

    private final VerticalsConfigService verticalConfigService;
    private final AggregationFacadeService aggregationFacadeService;

    public ScoreController(AggregationFacadeService aggregationFacadeService,
                           VerticalsConfigService verticalsConfigService) {
        this.verticalConfigService = verticalsConfigService;
        this.aggregationFacadeService = aggregationFacadeService;
    }

    @PostMapping("/score/{vertical}")
    @Operation(summary = "Score a specific vertical")
    public void scoreVertical(@PathVariable @NotBlank final String vertical)
            throws InvalidParameterException, IOException, InterruptedException {
        aggregationFacadeService.score(verticalConfigService.getConfigById(vertical));
    }

    @PostMapping("/score")
    @Operation(summary = "Score all verticals (sanitisation + launch the scheduled batch that scores all verticals)")
    public void scoreAll() throws InvalidParameterException, IOException, InterruptedException {
        aggregationFacadeService.scoreAll();
    }
}

package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing aggregated statistics for the frontend.
 */
@RestController
@RequestMapping("/stats")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Stats", description = "Aggregated catalogue statistics for the frontend UI")
public class StatsController {

    private static final CacheControl FIVE_MINUTES_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Get categories statistics",
            description = "Return aggregated statistics about vertical category mappings.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statistics returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CategoriesStatsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CategoriesStatsDto> categories() {
        CategoriesStatsDto body = statsService.categories();
        return ResponseEntity.ok()
                .cacheControl(FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }
}

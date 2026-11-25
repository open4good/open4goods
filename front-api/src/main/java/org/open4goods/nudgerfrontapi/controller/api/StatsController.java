package org.open4goods.nudgerfrontapi.controller.api;




import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing aggregated statistics for the frontend.
 *
 * <p>
 * Provides category-level analytics, dataviz chart presets, chart query
 * execution and hero KPI statistics for the Nuxt frontend dataviz pages.
 * </p>
 */
@RestController
@RequestMapping("/stats")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Stats", description = "Aggregated catalogue statistics for the frontend UI")
public class StatsController {

    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Get categories statistics",
            description = "Return aggregated statistics about vertical category mappings, affiliation partners and available OpenData items.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statistics returned",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CategoriesStatsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CategoriesStatsDto> categories(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering categories(domainLanguage={})", domainLanguage);
        CategoriesStatsDto body = statsService.categories(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }
}

package org.open4goods.nudgerfrontapi.controller.api;



import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesScoreStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesScoresStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartQueryRequestDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartQueryResponseDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizHeroStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalDatavizPlanDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.DatavizStatsService;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final DatavizStatsService datavizStatsService;

    public StatsController(StatsService statsService, DatavizStatsService datavizStatsService) {
        this.statsService = statsService;
        this.datavizStatsService = datavizStatsService;
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

    @GetMapping("/verticals/{verticalId}/dataviz/plan")
    @Operation(
            summary = "Get dataviz presets for a vertical",
            description = "Return the default filters and chart presets used by the frontend dataviz gallery for a vertical.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "verticalId", in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH, required = true,
                            description = "Vertical identifier resolved from YAML configuration.",
                            schema = @Schema(type = "string", example = "televisions")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataviz plan returned",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = VerticalDatavizPlanDto.class))),
                    @ApiResponse(responseCode = "404", description = "Vertical not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<VerticalDatavizPlanDto> datavizPlan(@PathVariable("verticalId") String verticalId,
                                                               @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering datavizPlan(verticalId={}, domainLanguage={})", verticalId, domainLanguage);
        VerticalDatavizPlanDto body = datavizStatsService.getVerticalPlan(verticalId, domainLanguage);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    /**
     * Execute a chart aggregation query for the given vertical and chart preset.
     *
     * @param verticalId     vertical identifier
     * @param request        chart query request containing chart ID and optional filter overrides
     * @param domainLanguage language for localised chart metadata
     * @return chart query response with labels and values ready for charting
     */
    @PostMapping("/verticals/{verticalId}/charts/query")
    @Operation(
            summary = "Execute a chart aggregation query",
            description = "Execute an Elasticsearch aggregation query for a specific chart preset and return labels/values arrays ready for the frontend charting library.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "verticalId", in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH, required = true,
                            description = "Vertical identifier scoping the query.",
                            schema = @Schema(type = "string", example = "televisions")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of chart titles and descriptions.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chart data returned",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = DatavizChartQueryResponseDto.class))),
                    @ApiResponse(responseCode = "404", description = "Vertical or chart preset not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<DatavizChartQueryResponseDto> chartQuery(
            @PathVariable("verticalId") String verticalId,
            @RequestBody DatavizChartQueryRequestDto request,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering chartQuery(verticalId={}, chartId={}, domainLanguage={})", verticalId, request.chartId(), domainLanguage);
        DatavizChartQueryResponseDto body = datavizStatsService.executeChartQuery(verticalId, request, domainLanguage);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    /**
     * Get hero-level KPI statistics for the dataviz page.
     *
     * @param verticalId     vertical identifier
     * @param domainLanguage language for localisation
     * @return hero statistics or 404 when the vertical is unknown
     */
    @GetMapping("/verticals/{verticalId}/dataviz/hero")
    @Operation(
            summary = "Get dataviz hero KPI statistics",
            description = "Return headline KPIs (total products, average price, top brand, etc.) displayed in the hero section of the dataviz page.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "verticalId", in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH, required = true,
                            description = "Vertical identifier.",
                            schema = @Schema(type = "string", example = "televisions")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = true,
                            description = "Language driving localisation.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Hero statistics returned",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = DatavizHeroStatsDto.class))),
                    @ApiResponse(responseCode = "404", description = "Vertical not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<DatavizHeroStatsDto> datavizHero(
            @PathVariable("verticalId") String verticalId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering datavizHero(verticalId={}, domainLanguage={})", verticalId, domainLanguage);
        DatavizHeroStatsDto body = datavizStatsService.computeHeroStats(verticalId, domainLanguage);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/categories/scores")
    @Operation(
            summary = "Get categories score cardinalities",
            description = "Return per-category score cardinalities for each available impact score criteria.",
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
                                    schema = @Schema(implementation = CategoriesScoresStatsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CategoriesScoresStatsDto> categoriesScores(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering categoriesScores(domainLanguage={})", domainLanguage);
        CategoriesScoresStatsDto body = statsService.categoryScores(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/categories/scores/{scoreName}")
    @Operation(
            summary = "Get category score cardinalities for a score",
            description = "Return per-category score cardinalities for a single impact score criteria.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "scoreName", in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH, required = true,
                            description = "Score name to fetch cardinalities for.",
                            schema = @Schema(type = "string", example = "ECOSCORE")),
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
                                    schema = @Schema(implementation = CategoriesScoreStatsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CategoriesScoreStatsDto> categoriesScore(
            @PathVariable("scoreName") String scoreName,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering categoriesScore(scoreName={}, domainLanguage={})", scoreName, domainLanguage);
        CategoriesScoreStatsDto body = statsService.categoryScore(domainLanguage, scoreName);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/random")
    @Operation(
            summary = "Get random products",
            description = "Return a list of random products.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "num", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = false,
                            description = "Number of products to return (max 10).",
                            schema = @Schema(type = "integer", defaultValue = "10", maximum = "10")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "minOffersCount", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = false,
                            description = "Minimum number of offers required for a product to be included.",
                            schema = @Schema(type = "integer", defaultValue = "3")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "verticalId", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = false,
                            description = "Optional vertical filter.",
                            schema = @Schema(type = "string")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Products returned",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ProductDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<ProductDto>> random(
            @RequestParam(name = "num", defaultValue = "10") int num,
            @RequestParam(name = "minOffersCount", defaultValue = "3") int minOffersCount,
            @RequestParam(name = "verticalId", required = false) String verticalId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        logger.info("Entering random(num={}, minOffersCount={}, verticalId={}, domainLanguage={})", num, minOffersCount, verticalId, domainLanguage);
        if (num > 10) {
            num = 10;
        }
        List<ProductDto> body = statsService.random(num, minOffersCount, verticalId, domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noCache())
                .body(body);
    }
}

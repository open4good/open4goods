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
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
                                    schema = @Schema(implementation = ProductDto.class))), // Should be ArraySchema but for simplicity using implementation
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
                .cacheControl(org.springframework.http.CacheControl.noCache()) // Random content should not be cached typically, or very short duration
                .body(body);
    }
}

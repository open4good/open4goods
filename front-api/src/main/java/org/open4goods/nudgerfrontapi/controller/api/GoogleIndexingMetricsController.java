package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.GoogleIndexingMetricsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.GoogleIndexingService;
import org.open4goods.nudgerfrontapi.service.dto.GoogleIndexingMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing Google Indexing queue metrics.
 */
@RestController
@RequestMapping("/metrics/google-indexing")
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Google Indexing", description = "Expose metrics for the Google Indexing queue.")
public class GoogleIndexingMetricsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexingMetricsController.class);

    private final GoogleIndexingService googleIndexingService;

    /**
     * Create the controller.
     *
     * @param googleIndexingService service providing indexing metrics
     */
    public GoogleIndexingMetricsController(GoogleIndexingService googleIndexingService) {
        this.googleIndexingService = googleIndexingService;
    }

    /**
     * Return the current Google Indexing queue metrics.
     *
     * @param domainLanguage domain language hint (required by API contract)
     * @return metrics response
     */
    @GetMapping
    @Operation(
            summary = "Get Google Indexing metrics",
            description = "Return queue metrics and activity timestamps for Google Indexing.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "domainLanguage", in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,
                            required = true,
                            description = "Language driving localisation of responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metrics returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GoogleIndexingMetricsDto.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<GoogleIndexingMetricsDto> metrics(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering metrics(domainLanguage={})", domainLanguage);
        GoogleIndexingMetrics metrics = googleIndexingService.metrics();
        GoogleIndexingMetricsDto payload = new GoogleIndexingMetricsDto(
                metrics.enabled(),
                metrics.pendingCount(),
                metrics.indexedCount(),
                metrics.deadLetterCount(),
                metrics.lastSuccessAt(),
                metrics.lastFailureAt(),
                metrics.batchSize(),
                metrics.retryDelay(),
                metrics.maxAttempts(),
                metrics.realtimeEnabled());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(payload);
    }
}

package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.indexation.GoogleIndexationMetricsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.GoogleIndexationQueueService;
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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoints exposing Google Indexation metrics.
 */
@RestController
@RequestMapping("/api/indexation")
@Tag(name = "Indexation", description = "Google Indexation queue metrics and status.")
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
public class GoogleIndexationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexationController.class);

    private final GoogleIndexationQueueService queueService;

    /**
     * Create the controller.
     *
     * @param queueService queue service for metrics
     */
    public GoogleIndexationController(GoogleIndexationQueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * Return the current Google Indexation queue metrics.
     *
     * @param domainLanguage requested domain language
     * @return metrics snapshot
     */
    @GetMapping("/metrics")
    @Operation(
            summary = "Get Google Indexation metrics",
            description = "Expose queue metrics and recent dispatch status for Google Indexation.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metrics returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string")))
            }
    )
    public ResponseEntity<GoogleIndexationMetricsDto> metrics(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering GoogleIndexationController.metrics(domainLanguage={})", domainLanguage);
        GoogleIndexationMetricsDto metrics = queueService.metricsSnapshot(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(metrics);
    }
}

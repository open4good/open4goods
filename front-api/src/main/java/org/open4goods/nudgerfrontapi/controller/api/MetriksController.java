package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.metriks.MetriksReportDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.metriks.MetriksReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * REST controller exposing metriks reports to the frontend.
 */
@RestController
@RequestMapping("/metriks")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Metriks", description = "Weekly KPI snapshots aggregated for frontend dashboards")
public class MetriksController {

    private static final Logger logger = LoggerFactory.getLogger(MetriksController.class);

    private final MetriksReportService reportService;

    public MetriksController(MetriksReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    @Operation(
            summary = "Get metriks report",
            description = "Return a column-oriented metriks report for the requested history period.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(name = "domainLanguage",
                            required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class)),
                    @io.swagger.v3.oas.annotations.Parameter(name = "limit",
                            description = "Maximum number of columns to return (latest dates).",
                            schema = @Schema(type = "integer", example = "12", minimum = "1")),
                    @io.swagger.v3.oas.annotations.Parameter(name = "includePayload",
                            description = "Include raw metriks payloads for hover usage.",
                            schema = @Schema(type = "boolean", example = "false"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report returned",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MetriksReportDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<MetriksReportDto> report(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "limit", defaultValue = "12") int limit,
            @RequestParam(name = "includePayload", defaultValue = "false") boolean includePayload) {
        logger.info("Metriks report requested: limit={}, includePayload={}", limit, includePayload);
        MetriksReportDto body = reportService.buildReport(limit, includePayload, domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .body(body);
    }
}

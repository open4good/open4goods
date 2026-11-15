package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.service.css.CssDryRunReport;
import org.open4goods.nudgerfrontapi.service.css.CssDryRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Internal administrative endpoints of the Google CSS dry run (GOU-2080). Only exists with
 * {@code front.css.enabled=true}; protected like the other {@code /admin} endpoints (site editor role).
 */
@RestController
@RequestMapping("/admin/css")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_EDITOR + "')")
@ConditionalOnProperty(prefix = "front.css", name = "enabled", havingValue = "true")
@Tag(name = "Administration", description = "Runtime administrative operations")
public class CssDryRunController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CssDryRunController.class);

    private final CssDryRunService service;

    /**
     * @param service CSS dry run service
     */
    public CssDryRunController(CssDryRunService service) {
        this.service = service;
    }

    /**
     * Starts a dry run in the background.
     *
     * @return 202 when started, 409 when a run is already in progress
     */
    @PostMapping("/dry-run")
    @Operation(summary = "Start the CSS dry run",
            description = "Writes the NDJSON files and the report in front.css.output-directory. Nothing is sent to Google.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Run started"),
                    @ApiResponse(responseCode = "409", description = "A run is already in progress"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated")
            })
    public ResponseEntity<Void> start() {
        LOGGER.info("Entering CssDryRunController.start()");
        return service.start() ? ResponseEntity.accepted().build() : ResponseEntity.status(409).build();
    }

    /**
     * Returns the report of the last completed run.
     *
     * @return the report, or 204 when no run completed since startup
     */
    @GetMapping("/dry-run")
    @Operation(summary = "Last CSS dry run report",
            description = "Eligible and examined products per vertical, with the exclusion counts by reason.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report returned"),
                    @ApiResponse(responseCode = "204", description = "No completed run since startup"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated")
            })
    public ResponseEntity<CssDryRunReport> report() {
        CssDryRunReport report = service.lastReport();
        return report == null ? ResponseEntity.noContent().build()
                : ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(report);
    }
}

package org.open4goods.api.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus;
import org.open4goods.services.opendata.service.OpenDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints of the open data generation (GOU-2248), moved from ui: manual start and status of the pass.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Open data", description = "Generation of the GTIN and ISBN open data zips and manifests. One pass at a "
		+ "time, in the background; scheduled weekly by open-data-config.generation-cron.")
public class OpenDataController {

	private final OpenDataService openDataService;

	public OpenDataController(OpenDataService openDataService) {
		this.openDataService = openDataService;
	}

	@PostMapping("/opendata/generate")
	@Operation(summary = "Start an open data generation pass",
			description = "Exports the GTIN and ISBN datasets from Elasticsearch (projected source, bounded pages, pause "
					+ "between pages) into a temporary folder, checks the free disk space and the row guard, then "
					+ "publishes atomically the zips, the per-dataset manifests and manifest.json last. A failure leaves "
					+ "the published files untouched and turns the open data health DOWN. The pass lasts hours: run it "
					+ "outside the batch window. The call is asynchronous and answers 202.")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Pass started"),
			@ApiResponse(responseCode = "409", description = "A pass is running, or generation is disabled by "
					+ "open-data-config.generation-enabled: current status returned")
	})
	public ResponseEntity<OpenDataGenerationStatus> generate() {
		boolean started = openDataService.launchGeneration();
		return ResponseEntity.status(started ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT).body(openDataService.status());
	}

	@GetMapping("/opendata/generation")
	@Operation(summary = "Status of the open data generation",
			description = "State of the current or last pass of this process (IDLE after a restart), its dates, the "
					+ "error of a failed pass and the rows of the last successful one.")
	@ApiResponse(responseCode = "200", description = "Generation status")
	public OpenDataGenerationStatus status() {
		return openDataService.status();
	}
}

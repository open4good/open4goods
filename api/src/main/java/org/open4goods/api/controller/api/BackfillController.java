package org.open4goods.api.controller.api;

import java.util.List;

import org.open4goods.api.services.backfill.BackfillOptions;
import org.open4goods.api.services.backfill.BackfillService;
import org.open4goods.api.services.backfill.BackfillStatus;
import org.open4goods.api.services.backfill.DecitrePurgeOperation;
import org.open4goods.api.services.backfill.NatureBackfillOperation;
import org.open4goods.model.RolesConstants;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints of the batched, resumable updates of the product index (GOU-2266): start, status, stop, resume and
 * restore. One run at a time, in the background; never scheduled.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Backfill", description = "Batched, resumable updates of a few fields of many products, with dry run, "
		+ "JSONL backup and restore. One run at a time, in the background, never scheduled.")
public class BackfillController {

	private final BackfillService backfillService;
	private final VerticalsConfigService verticalsConfigService;

	public BackfillController(BackfillService backfillService, VerticalsConfigService verticalsConfigService) {
		this.backfillService = backfillService;
		this.verticalsConfigService = verticalsConfigService;
	}

	@PostMapping("/batch/backfill/decitre-purge")
	@Operation(summary = "Purge the Decitre attribute keys from the indexed books",
			description = "Scans the products whose gtinInfos.upcType is ISBN_13 through a point in time, by batches, and "
					+ "removes the given keys from attributes.all, and only those, by a painless script: no other field "
					+ "is written, lastChange included. dryRun=true (default) only counts the products concerned and the "
					+ "occurrences of each key: run it first, it is the 'before' count. With dryRun=false, each removed "
					+ "attribute is first saved whole in a JSONL backup file (forced to disk), then removed. The run stops "
					+ "cleanly on POST /batch/backfill/stop or when Elasticsearch answers 429; resume=true continues "
					+ "it from its cursor, with the same backup file. The run is asynchronous: this call answers 202.")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Run started (or already running: current status returned)"),
			@ApiResponse(responseCode = "400", description = "Invalid batch size, rate or keys"),
			@ApiResponse(responseCode = "409", description = "resume=true but nothing to resume")
	})
	public ResponseEntity<BackfillStatus> purgeDecitre(
			@Parameter(description = "When true (default), nothing is written")
			@RequestParam(defaultValue = "true") boolean dryRun,
			@Parameter(description = "Products read per page and updated per bulk (1 to 5000)")
			@RequestParam(defaultValue = "1000") int batchSize,
			@Parameter(description = "Reading rate cap, in scanned products per second; 0 means no cap")
			@RequestParam(defaultValue = "2000") int maxProductsPerSecond,
			@Parameter(description = "Continue the last stopped, interrupted or failed run of the same mode from its cursor")
			@RequestParam(defaultValue = "false") boolean resume,
			@Parameter(description = "Exact keys of attributes.all to remove; defaults to the 7 Decitre keys excluded at "
					+ "ingestion (GOU-2264)")
			@RequestParam(required = false) List<String> keys) {
		try {
			DecitrePurgeOperation operation = keys == null || keys.isEmpty() ? new DecitrePurgeOperation()
					: new DecitrePurgeOperation(keys);
			BackfillOptions options = new BackfillOptions(dryRun, batchSize, maxProductsPerSecond, resume);
			return ResponseEntity.accepted().body(backfillService.start(operation, options));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} catch (IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
		}
	}

	@PostMapping("/batch/backfill/nature")
	@Operation(summary = "Set the nature (accessory, spare part) of the live products without vertical",
			description = "Scans the products without vertical whose lastChange is within lastChangeDays, with offers and "
					+ "merchant categories, and sets or removes their nature as the aggregation computes it (GOU-2233), "
					+ "with the given natureMinMarkedCategoryShare threshold. Only the nature field is written, and only "
					+ "when it differs; a product that got a vertical meanwhile is left untouched. dryRun=true (default) "
					+ "writes nothing and reports, for each report threshold, the products marked per nature value, and "
					+ "20 examples marked at the lowest report threshold but not at the highest (details of the report). "
					+ "No backup and no restore: the previous value is recomputable. Stop and resume as the other "
					+ "backfills. The run is asynchronous: this call answers 202.")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Run started (or already running: current status returned)"),
			@ApiResponse(responseCode = "400", description = "Invalid batch size, rate, threshold or window"),
			@ApiResponse(responseCode = "409", description = "resume=true but nothing to resume")
	})
	public ResponseEntity<BackfillStatus> backfillNature(
			@Parameter(description = "When true (default), nothing is written")
			@RequestParam(defaultValue = "true") boolean dryRun,
			@Parameter(description = "Products read per page and updated per bulk (1 to 5000)")
			@RequestParam(defaultValue = "1000") int batchSize,
			@Parameter(description = "Reading rate cap, in scanned products per second; 0 means no cap")
			@RequestParam(defaultValue = "2000") int maxProductsPerSecond,
			@Parameter(description = "Continue the last stopped, interrupted or failed run of the same mode from its cursor")
			@RequestParam(defaultValue = "false") boolean resume,
			@Parameter(description = "natureMinMarkedCategoryShare (0 to 1) of the nature; defaults to the value of "
					+ "_default.yml, the only one accepted with dryRun=false (the aggregation uses it too)")
			@RequestParam(required = false) Double threshold,
			@Parameter(description = "Thresholds counted in the report (0 to 1), default 0 and 0.5")
			@RequestParam(required = false) List<Double> reportThresholds,
			@Parameter(description = "lastChange window, in days: the live products")
			@RequestParam(defaultValue = "2") int lastChangeDays) {
		try {
			double configured = verticalsConfigService.getConfigByIdOrDefault(null).getNatureMinMarkedCategoryShare();
			if (!dryRun && threshold != null && Double.compare(threshold, configured) != 0) {
				// The aggregation recomputes the nature with the configured threshold: writing with another one would
				// make natures flip back on the next aggregation of each product
				throw new IllegalArgumentException("threshold " + threshold + " differs from natureMinMarkedCategoryShare "
						+ configured + " of _default.yml: set it there and deploy before writing");
			}
			double share = threshold != null ? threshold : configured;
			NatureBackfillOperation operation = new NatureBackfillOperation(verticalsConfigService, share,
					reportThresholds, lastChangeDays);
			BackfillOptions options = new BackfillOptions(dryRun, batchSize, maxProductsPerSecond, resume);
			return ResponseEntity.accepted().body(backfillService.start(operation, options));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} catch (IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
		}
	}

	@PostMapping("/batch/backfill/decitre-purge/restore")
	@Operation(summary = "Put back the Decitre attribute keys from a backup file",
			description = "Reads a JSONL backup file written by a decitre-purge run (file name only, inside the backfill "
					+ "backup folder, see the backupFile of the run status, named decitre-purge-...) and puts each saved "
					+ "attribute back into attributes.all of its product. The keys stay only if their ingestion "
					+ "exclusion (GOU-2264) is reverted too. Asynchronous: this call answers 202.")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Restore started (or a run is already in progress: current status returned)"),
			@ApiResponse(responseCode = "400", description = "Unknown backup file, or a path outside the backup folder")
	})
	public ResponseEntity<BackfillStatus> restoreDecitre(
			@Parameter(description = "Backup file name, for instance decitre-purge-20261116T220000Z.jsonl", required = true)
			@RequestParam String backupFile,
			@Parameter(description = "Products restored per bulk (1 to 5000)")
			@RequestParam(defaultValue = "1000") int batchSize,
			@Parameter(description = "Restore rate cap, in products per second; 0 means no cap")
			@RequestParam(defaultValue = "2000") int maxProductsPerSecond) {
		try {
			// Validates the batch size and the rate the same way as a run
			new BackfillOptions(false, batchSize, maxProductsPerSecond, false);
			return ResponseEntity.accepted().body(backfillService.startRestore(new DecitrePurgeOperation(), backupFile,
					batchSize, maxProductsPerSecond));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PostMapping("/batch/backfill/stop")
	@Operation(summary = "Stop the current backfill or restore",
			description = "The run stops after its current batch and ends STOPPED, with the cursor of the last batch "
					+ "fully handled; a backfill can then be resumed with resume=true.")
	@ApiResponse(responseCode = "200", description = "Current status")
	public BackfillStatus stop() {
		return backfillService.requestStop();
	}

	@GetMapping("/batch/backfill")
	@Operation(summary = "Status and report of the current or last backfill or restore",
			description = "IDLE, RUNNING (counters of the last batch), COMPLETED, STOPPED (report.stopReason: "
					+ "STOP_REQUESTED or BACKPRESSURE), FAILED (error) or INTERRUPTED (api restarted during the run). "
					+ "The report gives scanned, matched, written and failed products, the occurrences of each removed "
					+ "key (labelCounts), sample product ids, the backup file and the cursor.")
	@ApiResponse(responseCode = "200", description = "Current status")
	public BackfillStatus status() {
		return backfillService.getStatus();
	}
}

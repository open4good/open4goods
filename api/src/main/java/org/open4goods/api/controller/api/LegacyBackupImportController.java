package org.open4goods.api.controller.api;

import java.nio.file.Path;

import org.open4goods.api.services.migration.legacybackup.LegacyBackupApplyResult;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupImportRequest;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupImportService;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupInventoryReport;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupSampleReport;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupStatusReport;
import org.open4goods.model.RolesConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for the resumable legacy backup to neutral data importer.
 *
 * <p>No operation here ever runs automatically: every call is an explicit, operator-triggered
 * request naming a dataset id and its immutable pinned manifest. {@code APPLY} never touches the
 * active legacy Product repository or alias.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "LegacyBackupImport",
        description = "Resumable import of the pinned legacy product backup into neutral source, legacy baseline and price stores.")
public class LegacyBackupImportController {

    private final LegacyBackupImportService importService;

    public LegacyBackupImportController(LegacyBackupImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/migration/legacy-backup/inventory")
    @Operation(summary = "Validate the pinned manifest and every file's digest/line count",
            description = "Writes nothing. Rejects a changed, incomplete, or unsafely named archive file.")
    @ApiResponse(responseCode = "200", description = "Inventory report")
    public LegacyBackupInventoryReport inventory(
            @Parameter(description = "Bounded dataset identifier for this pinned import run", required = true)
            @RequestParam String datasetId,
            @Parameter(description = "Absolute path to the pinned input manifest", required = true)
            @RequestParam String manifestPath) {
        return importService.inventory(new LegacyBackupImportRequest(datasetId, Path.of(manifestPath)));
    }

    @PostMapping("/migration/legacy-backup/sample")
    @Operation(summary = "Dry-run classify a bounded sample of the first archive file's lines",
            description = "Writes nothing. Useful to preview conversion, price and dead-letter counts before APPLY.")
    @ApiResponse(responseCode = "200", description = "Sample report")
    public LegacyBackupSampleReport sample(
            @RequestParam String datasetId,
            @RequestParam String manifestPath,
            @Parameter(description = "Number of leading lines of the first file to classify")
            @RequestParam(defaultValue = "500") long sampleLines) {
        return importService.sample(new LegacyBackupImportRequest(datasetId, Path.of(manifestPath)), sampleLines);
    }

    @PostMapping("/migration/legacy-backup/apply")
    @Operation(summary = "Resumable write of source, legacy baseline and price stores",
            description = "Resumes from the last committed checkpoint. Never calls BackupService.importProducts or writes "
                    + "the active legacy Product repository/alias.")
    @ApiResponse(responseCode = "200", description = "Apply result for this invocation")
    public LegacyBackupApplyResult apply(
            @RequestParam String datasetId,
            @RequestParam String manifestPath) {
        return importService.apply(new LegacyBackupImportRequest(datasetId, Path.of(manifestPath)));
    }

    @GetMapping("/migration/legacy-backup/status")
    @Operation(summary = "Report checkpoint progress for a dataset", description = "Read-only; touches no dataset file.")
    @ApiResponse(responseCode = "200", description = "Status report")
    public LegacyBackupStatusReport status(@RequestParam String datasetId) {
        return importService.status(datasetId);
    }

    @PostMapping("/migration/legacy-backup/cancel")
    @Operation(summary = "Request a running APPLY to stop after its current batch")
    @ApiResponse(responseCode = "200", description = "Cancellation requested")
    public void cancel(@RequestParam String datasetId) {
        importService.cancel(datasetId);
    }
}

package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.backup.BackupService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for manual triggering of data backup and recovery operations.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Backup", description = "Manual backup and restore operations for XWiki content and Elasticsearch product data.")
public class BackupController {

	private final BackupService backupService;

	public BackupController(BackupService backupService) {
		this.backupService = backupService;
	}

	@PostMapping("/backup/xwiki")
	@Operation(
			summary = "Launch a XWiki backup",
			description = "Triggers a full backup of the XWiki content (brand pages, vertical descriptions, guides). "
					+ "The backup is written to the configured backup directory on the server filesystem.")
	@ApiResponse(responseCode = "200", description = "XWiki backup started")
	public void xwikiBackup() throws InvalidParameterException, IOException {
		backupService.backupXwiki();
	}

	@PostMapping("/backup/products/export")
	@Operation(
			summary = "Export all products to backup",
			description = "Streams the full Elasticsearch product index to the configured backup directory. "
					+ "This operation can be long-running on large indices. "
					+ "Use /backup/products/export/vertical to export a single vertical instead.")
	@ApiResponse(responseCode = "200", description = "Product export started")
	public void productsExport() throws InvalidParameterException, IOException {
		backupService.backupProducts();
	}

	@PostMapping("/backup/products/export/vertical")
	@Operation(
			summary = "Export products for a specific vertical to backup",
			description = "Streams all products belonging to the given vertical from Elasticsearch to the backup directory. "
					+ "Faster than a full export when only one category needs to be backed up or transferred.")
	@ApiResponse(responseCode = "200", description = "Vertical product export started")
	public void productsExport(
			@Parameter(description = "Vertical identifier whose products should be exported (e.g. 'tv', 'laptop')", required = true)
			@RequestParam String vertical) throws InvalidParameterException, IOException {
		backupService.exportVertical(vertical);
	}

	@PostMapping("/backup/products/import")
	@Operation(
			summary = "Import products from the latest backup",
			description = "Reads the product backup files from the configured backup directory and bulk-indexes them "
					+ "back into Elasticsearch. Use with care: existing documents with the same GTIN will be overwritten.")
	@ApiResponse(responseCode = "200", description = "Product import started")
	public void productsImport() throws InvalidParameterException, IOException {
		backupService.importProducts();
	}

	@PostMapping("/backup/products/copy-to")
	@Operation(
			summary = "Copy the product index to a new index with the given suffix",
			description = "Creates a copy of the main Elasticsearch product index under a name composed of the base "
					+ "index name plus the supplied suffix (e.g. '_backup_20260101'). "
					+ "Useful for creating point-in-time snapshots before a batch migration.")
	@ApiResponse(responseCode = "200", description = "Index copy operation started")
	public void copyTo(
			@Parameter(description = "Suffix appended to the base index name for the destination index", required = true)
			@RequestParam String suffix) throws InvalidParameterException, IOException {
		backupService.copyTo(suffix);
	}
}


package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.BackupService;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.RolesConstants;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * This controller handle manual triggering of datas backup and recovery
 * 
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Profile("!beta")
public class BackupController {

	private final BackupService backupService;


	public BackupController( BackupService backupService) {
		this.backupService = backupService;
	}

	@PostMapping("/backup/xwiki")
	@Operation(summary = "Launch a Xwiki backup")
	public void xwikiBackup() throws InvalidParameterException, IOException {
		backupService.backupXwiki();
	}
	
	@PostMapping("/backup/products/export")
	@Operation(summary = "Launch a product backup")
	public void productsExport() throws InvalidParameterException, IOException {
		backupService.backupProducts();
	}

	@PostMapping("/backup/products/import")
	@Operation(summary = "Launch a product import")
	public void productsImport() throws InvalidParameterException, IOException {
		backupService.importProducts();
	}
}

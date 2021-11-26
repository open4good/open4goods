/**
 * This controller allow export operations.
 *
 * @author gof
 *
 */

package org.open4goods.api.controller.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.open4goods.api.services.ImportExportService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController("Administration")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class ExportController {

	@Autowired
	private ImportExportService exportService;

	
	@Autowired DataFragmentRepository datafragmentsRepository;
	@Autowired DataFragmentStoreService storeService;
	
	@Autowired AggregatedDataRepository aggDataRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
	

	@GetMapping("/backup/aggregateddatas/")
	public void backupDatas() throws InvalidParameterException, IOException {
		aggDataRepository.backup();
		
		
	}
	
	
	@GetMapping("/import/datafragments/")
	public void importDatafragments() throws InvalidParameterException, IOException {
		
		datafragmentsRepository.export("*").forEach(e -> {
			storeService.queueDataFragment(e);
			
		});
		
		
	}

	
	

	@GetMapping("/api/admin/export/datafragments/")
	public void doExportFull(@RequestParam(defaultValue = "1000",name = "max",required = false) Long maxItem, HttpServletResponse response) throws InvalidParameterException, IOException {
		exportService.dataFragmentsfragmentsToHttpResponse(response, "datafragments", maxItem);
	}



//
	@GetMapping("/api/admin/backup/datafragments/")
	public void doExportFull() throws InvalidParameterException {
		exportService.exportAndCleanup();
	}

//	@GetMapping("/api/admin/export/datafragments/medium")
//	public void doExportFull() throws InvalidParameterException {
//		exportService.exportAndCleanup();
//	}
//
//	@GetMapping("/api/admin/export/datafragments/small")
//	public void doExportFull() throws InvalidParameterException {
//		exportService.exportAndCleanup();
//	}
//
//	
//	@GetMapping("/api/admin/export/vertical/{verticalName}")
//	public void doExportFull() throws InvalidParameterException {
//		exportService.exportAndCleanup();
//	}
//

	@GetMapping("/api/import")
	public void importation() throws InvalidParameterException {
		exportService.doImport();
	}

}

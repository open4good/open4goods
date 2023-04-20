/**
 * This controller allow export operations.
 *
 * @author gof
 *
 */

package org.open4goods.api.controller.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.open4goods.api.services.BatchService;
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

@RestController("batch")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class BatchController {

	@Autowired
	private BatchService batchService;

	@Autowired AggregatedDataRepository aggDataRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
	

	@GetMapping("/batch/verticals/associate")
	public void backupDatas() throws InvalidParameterException, IOException {
		batchService.definesVertical();
		
		
	}
	

}

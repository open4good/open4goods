/**
 * This controller allow export operations.
 *
 * @author gof
 *
 */

package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.BatchService;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.RolesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("batch")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class BatchController {

	@Autowired
	private BatchService batchService;

	@Autowired ProductRepository aggDataRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
	

	@GetMapping("/batch/verticals/associateFromCategory")
	public void backupDatas() throws InvalidParameterException, IOException {
		batchService.definesVertical();
	}
	
	
	@GetMapping("/batch/verticals/score")
	public void scoreVerticals() throws InvalidParameterException, IOException {
		batchService.scoreVertical();
		
		
	}

}

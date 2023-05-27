/**
 * This controller allow export operations.
 *
 * @author gof
 *
 */

package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.RolesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("Administration")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class ExportController {

	
	@Autowired DataFragmentStoreService storeService;
	
	@Autowired AggregatedDataRepository aggDataRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
	

	@GetMapping("/backup/aggregateddatas/")
	public void backupDatas() throws InvalidParameterException, IOException {
		aggDataRepository.backup();
	}
	

}

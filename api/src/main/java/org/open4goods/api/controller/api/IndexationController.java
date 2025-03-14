
package org.open4goods.api.controller.api;

import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.model.dto.api.IndexationResponse;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_CRAWLER+"')")
@Hidden
public class IndexationController {

	private static final Logger logger = LoggerFactory.getLogger(IndexationController.class);


	private final DataFragmentStoreService storeService;	

	
	

	public IndexationController(DataFragmentStoreService storeService) {
		super();
		this.storeService = storeService;
	}	

	@PostMapping(path = UrlConstants.API_INDEXATION_ENDPOINT)
	//TODO(perf,0.75, p2) : could optimize with "bulk" frow crawler
	public IndexationResponse index(@RequestBody final DataFragment data) throws ValidationException {
		storeService.queueDataFragment(data);
		return new IndexationResponse();
	}



}

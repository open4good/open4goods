
package org.open4goods.api.controller.api;

import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.dto.api.IndexationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.annotations.ApiIgnore;

@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_CRAWLER+"')")
@ApiIgnore
public class IndexationController {

	private static final Logger logger = LoggerFactory.getLogger(IndexationController.class);

	@Autowired
	private DataFragmentStoreService storeService;


	@PostMapping(path = UrlConstants.API_INDEXATION_ENDPOINT)
	//TODO(perf,0.75, p2) : could optimize with "bulk" frow crawler
	public IndexationResponse index(@RequestBody final DataFragment data) {
		storeService.queueDataFragment(data);
		return new IndexationResponse();
    }

}

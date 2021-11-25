/**
 * This controller allow export operations.
 *
 * @author gof
 *
 */

package org.open4goods.api.controller.api;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.services.SerialisationService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StatsController {

	@Autowired private DataFragmentRepository datafragmentsRepository;

	@Autowired private DataFragmentStoreService datafragmentsStoreService;
	
	@Autowired private ApiProperties config;


	
	@Autowired SerialisationService serialisationService;

	private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

	@GetMapping("/api/stats/count")
	public Long fragmentsCount() {
		return datafragmentsRepository.count();
	}

	
	@GetMapping("/api/filequeue/count")
	public Long fileQueuecount() {
		return datafragmentsStoreService.getFileQueue().size();
	}
	
	@PostMapping("/api/filequeue/gc")
	public void gc() {
		datafragmentsStoreService.getFileQueue().gc();
	}
	


}

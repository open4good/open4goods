
package org.open4goods.api.controller.api;

import java.util.List;
import java.util.Map;

import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.websocket.server.PathParam;

/**
 * This controller handle manual triggering of datas backup and recovery
 *
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class EprelController {

	private EprelSearchService eprelSearchService ;
	private EprelCatalogueService eprelCatalogueService ;


	public EprelController( EprelSearchService eprelSearchService,EprelCatalogueService eprelCatalogueService) {
		this.eprelSearchService = eprelSearchService;
		this.eprelCatalogueService=eprelCatalogueService;
	}


	@GetMapping("/eprel/index")
	@Operation(summary = "Launch the eprel catalogue indexation")
	public void eprelIndex() {
		eprelCatalogueService.refreshCatalogue();

	}


	@GetMapping("/eprel/search")
	@Operation(summary = "Search by GTIN or model")
	public List<EprelProduct> eprelIndex(@RequestParam String gtin, @RequestParam String model) {
		return eprelSearchService.search(gtin, model);

	}



}

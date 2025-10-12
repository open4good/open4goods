
package org.open4goods.api.controller.api;

import java.util.Map;

import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.VerticalConfig;
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
@Profile("!beta")
public class IcecatController {

	private IcecatService icecatService = null;
	private final VerticalsConfigService verticalsService;


	public IcecatController( IcecatService icecatService,VerticalsConfigService verticalsService) {
		this.icecatService = icecatService;
		this.verticalsService=verticalsService;
	}


	/**
	 * Resolve the icecat features id, and apply the english name if an unconflicted match is found.
	 * The resolution is operated on the vertical matching features id if set, on all features id if not set 
	 * @param name
	 * @return
	 */
	@GetMapping("/feature/resolve")
	@Operation(summary = "Resolve the icecat features id, and apply the english name if an unconflicted match is found")
	public String getOriginalEnglishName(@RequestParam String name, @RequestParam String vertical) {
		VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
		return icecatService.getOriginalEnglishName(name, vc);
		
	}
	
	
	@GetMapping("/{vertical}/featuregroups/")
	@Operation(summary = " Loads the list of features, aggegated by UiFeatureGroup")
	public Map<String, String> getFeaturesGroup( @PathVariable String vertical) {
		VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
		return icecatService.types(vc);
		
	}
	
	@GetMapping("/features/{featuresId}/")
	@Operation(summary = " Loads the Feature for a given id")
	public IcecatFeature getFeature( @PathVariable Integer featuresId) {
		return icecatService.getFeaturesById().get(featuresId) ;
		
	}
	
	
	
	
	

		
	
}

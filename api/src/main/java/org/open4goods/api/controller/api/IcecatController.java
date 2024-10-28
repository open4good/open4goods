
package org.open4goods.api.controller.api;

import java.util.Map;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.model.icecat.IcecatFeature;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
	public Map<String, String> getFeaturesGroup( @PathParam(value = "vertical") String vertical) {
		VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
		return icecatService.types(vc);
		
	}
	
	@GetMapping("/features/{featuresId}/")
	@Operation(summary = " Loads the Featurez for a given id")
	public IcecatFeature getFeature( @PathParam(value = "featuresId") Integer featuresId) {
		return icecatService.getFeaturesById().get(featuresId) ;
		
	}
	
	
	
	
	

		
	
}

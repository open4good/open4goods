

package org.open4goods.api.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.api.services.VerticalsGenerationService;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class BrandController {

	private BrandService brandService;
	
	public BrandController(BrandService brandService) {
		super();
		this.brandService=brandService;
	}


	@GetMapping("/brands/stats/companies/missing")
	@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
	public LinkedHashMap<String,String> statsMissingCompanies(HttpServletRequest request) {
	    // Fetch the stats map
	    Map<String, Long> stats = brandService.getMissCounter();

	    // Sort the map by value in descending order
	   return  stats.entrySet().stream()
	            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
	            .collect(Collectors.toMap(
	                    Map.Entry::getKey,
	                    entry -> "", // Replace the value with an empty string
	                    (e1, e2) -> e1, // In case of duplicates, keep the first one
	                    LinkedHashMap::new // Maintain the order
	            ));

	}
}

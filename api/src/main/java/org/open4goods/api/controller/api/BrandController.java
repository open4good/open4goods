

package org.open4goods.api.controller.api;

import java.util.List;
import java.util.Map;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.BrandService;
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
	public List<String> statsMissingCompanies(HttpServletRequest request) {
	    // Fetch the stats map
	    Map<String, Long> stats = brandService.getMissCounter();

	    // Sort the map by value in descending order
	   return  stats.entrySet().stream()
	            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
	            .map(e->e.getKey())
	            .toList();

	}
}

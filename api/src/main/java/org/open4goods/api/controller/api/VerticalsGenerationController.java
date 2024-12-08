

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.open4goods.api.model.VerticalAttributesStats;
import org.open4goods.api.model.VerticalCategoryMapping;
import org.open4goods.api.services.VerticalsGenerationService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class VerticalsGenerationController {

	private  VerticalsGenerationService verticalsGenService;
	private  VerticalsConfigService verticalsConfigService;
	
	public VerticalsGenerationController(VerticalsGenerationService verticalsGenService, VerticalsConfigService verticalsConfigService) {
		super();
		this.verticalsGenService = verticalsGenService;
		this.verticalsConfigService = verticalsConfigService;
	}

	@GetMapping(path="/fullFromDb")
	@Operation(summary="Loads the mappings from database, then clean and save to file")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void full() throws ResourceNotFoundException, IOException {
		verticalsGenService.fullFromDb();
	}

	@GetMapping(path="/mappings/load/database")
	@Operation(summary="Loads the mappings from database")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void load() throws ResourceNotFoundException {
		verticalsGenService.loadCategoriesMappingFromDatabase();
	}

	@GetMapping(path="/mappings/load/file")
	@Operation(summary="Import the mapping file from JSON")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void importMapping() throws ResourceNotFoundException, IOException {
		verticalsGenService.importMappingFile();
	}
	
	
//	@GetMapping(path="/mappings/clean/threshold")
//	@Operation(summary="Clean the mappings by configured threshold (percent of totalhits an associated category must have to be retained)")
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
//	public void cleanThreshold() throws ResourceNotFoundException, IOException {
//		verticalsGenService.removeByAssociatedcategoryThreshold();
//	}
//
//	@GetMapping(path="/mappings/clean/crosslinked")
//	@Operation(summary="Clean the mappings by removing cross linked mappings)")
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
//	public void cleanCrossLinked() throws ResourceNotFoundException, IOException {
//		verticalsGenService.removeCrossReferencedMappings();
//	}
	
	

	@GetMapping(path="/mappings")
	@Operation(summary="Show the mappings")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Map<String, VerticalCategoryMapping> get() throws ResourceNotFoundException {
		return verticalsGenService.getMappings();
		
	}

	@GetMapping(path="/mappings/export")
	@Operation(summary="Export the mapping file to JSON")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void exportMapping() throws ResourceNotFoundException, IOException {
		 verticalsGenService.exportMappingFile();
		
	}
	

	@GetMapping(path="/assist/attributes/{vertical}")
	@Operation(summary="Generate attributes coverage for a vertical")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public VerticalAttributesStats generateAttributesCoverage(@PathVariable String vertical) throws ResourceNotFoundException, IOException {
		 return verticalsGenService.attributesStats(vertical);
		
	}
	

	@GetMapping(path="/assist/categories/gtin")
	@Operation(summary="Generate the categories yaml fragment for a given match")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public String generateCategoryMappingsFragment(@RequestParam String gtins) throws ResourceNotFoundException, IOException {
		 return verticalsGenService.generateCategoryMappingFragmentForGtin(Arrays.asList(gtins.split(",")), null );
		
	}
	
	
	@GetMapping(path="/assist/categories/vertical")
	@Operation(summary="Generate the categories yaml fragment for a given vertical")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public String generateCategoryMappingsForExistinf(@RequestParam String vertical, @RequestParam(defaultValue = "5") Integer minOffersCount ) throws ResourceNotFoundException, IOException {

		VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
		return verticalsGenService.generateMapping(vc,minOffersCount);
		
	}
	
	
	
	
	
	
	@GetMapping(path="/assist/vertical")
	@Operation(summary="Generate the vertical file")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void generateCategoryMappingsFragment( 
			@RequestParam String googleTaxonomyId,
			@RequestParam										 String matchingCategories,
			@RequestParam										 String urlPrefix,
			@RequestParam										 String h1Prefix,
			@RequestParam										 String verticalHomeUrl,
			@RequestParam										 String verticalHomeTitle) throws ResourceNotFoundException, IOException {
		
		  verticalsGenService.verticalTemplatetoFile(googleTaxonomyId, matchingCategories, urlPrefix, h1Prefix, verticalHomeUrl, verticalHomeTitle);
		
	}
	
	

	
	@GetMapping(path="/update/verticals/categoriesmapping")
	@Operation(summary="Update the categories mapping directly in the files !")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void updateVerticalsWithMappings( 
			@RequestParam	(defaultValue = "3")									 Integer minOffers) throws ResourceNotFoundException, IOException {
		
		//TODO(p2,conf) : from conf
 		  verticalsGenService.updateVerticalsWithMappings("/home/goulven/git/open4goods/verticals/src/main/resources/verticals/",minOffers);
		
	}
	
	@GetMapping(path="/update/verticals/categoriesmapping/{vertical}")
	@Operation(summary="Update the categories mapping for a given vertical directly in the file !")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void updateVerticalWithMappings( 
			@RequestParam	(defaultValue = "tv")									 String vertical,
			@RequestParam	(defaultValue = "3")									 Integer minOffers) throws ResourceNotFoundException, IOException {
		
		//TODO(p2,conf) : from conf
 		  verticalsGenService.updateVerticalFile(minOffers, "/home/goulven/git/open4goods/verticals/src/main/resources/verticals/"+vertical+".yml");
		
	}
	
	@GetMapping(path="/update/verticals/attributes/{vertical}")
	@Operation(summary="Update the suggested attributes for a given vertical directly in the file !")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void updateVerticalWithAttributes( 
			@RequestParam	(defaultValue = "tv")									 String vertical) throws ResourceNotFoundException, IOException {
		
		//TODO(p2,conf) : from conf
 		  verticalsGenService.generateAttributesMapping(verticalsConfigService.getConfigById(vertical));
		
	}
	
}

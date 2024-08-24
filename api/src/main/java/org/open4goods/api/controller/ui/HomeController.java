
package org.open4goods.api.controller.ui;


import java.util.List;

import org.open4goods.commons.model.dto.ExpandedTaxonomy;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import io.swagger.v3.oas.annotations.Hidden;

@Controller

//TODO(0.25, P2,design) Endpoints are badly named. Standardize, resfulize
@Hidden
public class HomeController {
	
	private @Autowired VerticalsConfigService verticalsConfigService;;

	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

	//	@Autowired DataFragmentRepository repository;


	//
	//
	//	@Autowired SerialisationService serialisationService;
	//	private @Autowired CapsuleGenerationService siteService;
	//	private @Autowired ApiProperties apiProperties;






	@GetMapping(path = "/")
	//	@ResponseBody
	public RedirectView  home() {

		return new RedirectView("swagger-ui.html");

	}

	
	@GetMapping(path = "/taxonomies")
	//	@ResponseBody
	public ModelAndView  taxonomies() {

		List<ExpandedTaxonomy> taxonomies = verticalsConfigService.expandedTaxonomies();
		
		
		ModelAndView mv = new ModelAndView("taxonomy.html");
		
		mv.addObject("taxonomies",taxonomies);
		
		return mv;

	}
	
	

}


package org.open4goods.api.controller.ui;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import io.swagger.v3.oas.annotations.Hidden;

@Controller

//TODO(0.25, P2,design) Endpoints are badly named. Standardize, resfulize
@Hidden
public class HomeController {

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


}


package org.open4goods.api.controller.ui;


import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import io.swagger.v3.oas.annotations.Hidden;

@Controller

@Hidden
public class HomeController {

	private @Autowired VerticalsConfigService verticalsConfigService;;

	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);


	@GetMapping(path = "/")
	//	@ResponseBody
	public RedirectView  home() {

		return new RedirectView("swagger-ui.html");

	}






}


package org.open4goods.nudgerfrontapi.controller.ui;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import io.swagger.v3.oas.annotations.Hidden;

@Controller

@Hidden
public class HomeController {


	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);


	@GetMapping(path = "/")
	public RedirectView  home() {
		return new RedirectView("/swagger-ui/index.html");

	}


}

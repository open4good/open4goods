package org.open4goods.ui.controllers.ui;

import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class TeamController extends AbstractUiController {

	final UiConfig uiConfig;

	public TeamController(UiConfig uiConfig) {
		this.uiConfig = uiConfig;
	}

	@GetMapping("/equipe")
	public ModelAndView index(final HttpServletRequest request) {

		ModelAndView model = defaultModelAndView("team", request);
		return model;
	}


}

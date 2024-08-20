package org.open4goods.ui.controllers.ui.pages;

import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class TeamController  implements SitemapExposedController{

	public static final String DEFAULT_PATH="/team";
	public static final String FR_PATH="/equipe";
	
	 @Autowired UiService uiService;
	 @Autowired UiConfig uiConfig;

	public TeamController() {
	}

	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY)
						   .add(SitemapEntry.LANGUAGE_FR, FR_PATH);
	}

	
	@GetMapping(value = {DEFAULT_PATH, FR_PATH})
	public ModelAndView index(final HttpServletRequest request) {

		ModelAndView model = uiService.defaultModelAndView("team", request);
		
		model.addObject("coreMembers", uiConfig.getTeamConfig().getCores());
		model.addObject("contributors", uiConfig.getTeamConfig().getContributors());
		
		
		return model;
	}



}

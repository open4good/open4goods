package org.open4goods.ui.controllers.ui.pages;

import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;

@Controller
/**
 * This controller maps the index page
 *
 * @author gof
 *
 */
public class EcoscoreController implements SitemapExposedController{

	public static final String DEFAULT_PATH="/ecoscore";
	public static final String FR_PATH="/ecoscore";
		
		
	private static final Logger LOGGER = LoggerFactory.getLogger(EcoscoreController.class);
	private @Autowired UiService uiService;
	// The siteConfig
	private final UiConfig config;

	public EcoscoreController(UiConfig config) {
		this.config = config;
	}


	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY)
						   .add(SitemapEntry.LANGUAGE_FR, FR_PATH);
	}

	
	@GetMapping(value = {DEFAULT_PATH, FR_PATH})
	public ModelAndView api(final HttpServletRequest request) {
		ModelAndView ret = uiService.defaultModelAndView(("ecoscore"), request);
		ret.addObject("page","évaluation environnementale");
		return ret;
	}

}
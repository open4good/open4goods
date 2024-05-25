package org.open4goods.ui.controllers.ui.pages;

import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.redfin.sitemapgenerator.ChangeFreq;

import jakarta.servlet.http.HttpServletRequest;

@Controller
/**
 * This controller maps the index page
 *
 * @author gof
 *
 */
public class EcologicalCashbackController implements SitemapExposedController{

	public static final String DEFAULT_PATH="/ecological-compensation";
	public static final String FR_PATH="/compensation-ecologique";
		
		

	private static final Logger LOGGER = LoggerFactory.getLogger(EcologicalCashbackController.class);
	private @Autowired UiService uiService;
	// The siteConfig
	private final UiConfig config;

	public EcologicalCashbackController(UiConfig config) {
		this.config = config;
	}


	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY)
						   .add(SitemapEntry.LANGUAGE_FR, FR_PATH);
	}

	
	@GetMapping(value = {DEFAULT_PATH, FR_PATH})
	public ModelAndView compensation(final HttpServletRequest request) {
		ModelAndView ret = uiService.defaultModelAndView(("compensation"), request);
		ret.addObject("page","compensation écologique");
		return ret;
	}


}
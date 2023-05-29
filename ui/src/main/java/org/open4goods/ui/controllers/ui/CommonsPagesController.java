package org.open4goods.ui.controllers.ui;

import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.mashape.unirest.http.exceptions.UnirestException;

import jakarta.servlet.http.HttpServletRequest;

@Controller
/**
 * This controller maps the index page
 *
 * @author gof
 *
 */
public class CommonsPagesController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonsPagesController.class);

	// The siteConfig
	private @Autowired UiConfig config;

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * The Home page.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws UnirestException
	 */


	@GetMapping(path= "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public ModelAndView robots(final HttpServletRequest request) {
		final ModelAndView ret = defaultModelAndView("robots", request);
		return ret;
	}

	@GetMapping(path = "/opensearch.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public ModelAndView opensearch(final HttpServletRequest request) {
		return defaultModelAndView("opensearch", request).addObject("opensearch", config.getOpenSearchConfig());
	}

	@GetMapping("/compensation-carbone")
	public ModelAndView compensation(final HttpServletRequest request) {
		ModelAndView ret = defaultModelAndView(("compensation"), request);
		ret.addObject("page","compensation écologique");
		return ret;
	}

	@GetMapping("/ecoscore")
	public ModelAndView api(final HttpServletRequest request) {
		ModelAndView ret = defaultModelAndView(("ecoscore"), request);
		ret.addObject("page","évaluation environnementale");
		return ret;
	}

}
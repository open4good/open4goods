package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.mashape.unirest.http.exceptions.UnirestException;

@Controller
/**
 * This controller maps the verticals pages
 *
 * @author gof
 *
 */
public class VerticalController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalController.class);

	// The siteConfig
	private @Autowired UiConfig config;
	
	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	@GetMapping("/tv")
	public ModelAndView partenaires(final HttpServletRequest request) {
		ModelAndView ret = defaultModelAndView(("vertical-home"), request);
		return ret;
	}

}
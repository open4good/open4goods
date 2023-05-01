package org.open4goods.ui.controllers.ui;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.dto.VerticalFilterTerm;
import org.open4goods.ui.controllers.dto.VerticalSearchRequest;
import org.open4goods.ui.controllers.dto.VerticalSearchResponse;
import org.open4goods.ui.services.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import com.ibm.icu.util.ULocale;

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
	
	private @Autowired VerticalsConfigService verticalService;
	
	private @Autowired SearchService searchService;
	
	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////



	public ModelAndView home(String vertical, final HttpServletRequest request) {
		ModelAndView ret = defaultModelAndView(("vertical-home"), request);
		

		VerticalConfig config = verticalService.getLanguageForVerticalPath(vertical);
				
		// TODO : strategy of injection of products for nativ SEO
		
		VerticalSearchRequest vRequest = new VerticalSearchRequest();		
		
		VerticalSearchResponse products = searchService.verticalSearch(config,vRequest);
		
		Map<String,String> countryNames = new HashMap<>();		
		for (VerticalFilterTerm country : products.getCountries()) {
			countryNames.put(country.getText(), new ULocale("",country.getText()).getDisplayCountry( new ULocale(request.getLocale().toString())));
		}

		
		
		ret.addObject("countryNames", countryNames);
		ret.addObject("products", products);
		
		ret.addObject("config",config);
		
		ret.addObject("filters",config.verticalFilters());
		
		
		
		
		// TODO: i18n
		ret.addObject("verticalPath",verticalService.getPathForVerticalLanguage("fr"));
		
		
		
		return ret;
	}

}
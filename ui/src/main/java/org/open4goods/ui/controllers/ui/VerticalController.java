package org.open4goods.ui.controllers.ui;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.dto.VerticalFilterTerm;
import org.open4goods.model.dto.VerticalSearchRequest;
import org.open4goods.model.dto.VerticalSearchResponse;
import org.open4goods.services.SearchService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import com.ibm.icu.util.ULocale;

import jakarta.servlet.http.HttpServletRequest;

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
	private final UiConfig config;

	private final VerticalsConfigService verticalService;

	private final SearchService searchService;


	private final SerialisationService serialisationService;

	public VerticalController(UiConfig config, VerticalsConfigService verticalService, SearchService searchService, SerialisationService serialisationService) {
		this.config = config;
		this.verticalService = verticalService;
		this.searchService = searchService;
		this.serialisationService = serialisationService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////



	public ModelAndView home(String vertical, final HttpServletRequest request) {
		ModelAndView ret = defaultModelAndView(("vertical-home"), request);


		VerticalConfig config = verticalService.getVerticalForPath(vertical);

		// TODO : strategy of injection of products for nativ SEO

		VerticalSearchRequest vRequest = new VerticalSearchRequest();

		// TODO : do not fetch any
//		VerticalSearchResponse products = searchService.verticalSearch(config,vRequest);
		VerticalSearchResponse products = new VerticalSearchResponse();

		
		
		Map<String,String> countryNames = new HashMap<>();
		for (VerticalFilterTerm country : products.getCountries()) {
			countryNames.put(country.getText(), new ULocale("",country.getText()).getDisplayCountry( new ULocale(request.getLocale().toString())));
		}



		ret.addObject("countryNames", countryNames);
		ret.addObject("products", products);

		ret.addObject("config",config);

		ret.addObject("filters",config.verticalFilters());
		ret.addObject("vertical",vertical);
		// TODO: i18n
		ret.addObject("verticalPath",verticalService.getPathForVerticalLanguage("fr",config));



		return ret;
	}

}
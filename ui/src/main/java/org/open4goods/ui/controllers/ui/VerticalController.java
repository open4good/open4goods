package org.open4goods.ui.controllers.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.attribute.AttributeType;
import org.open4goods.commons.model.dto.NumericRangeFilter;
import org.open4goods.commons.model.dto.VerticalFilterTerm;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.ui.services.BlogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.ibm.icu.util.ULocale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller maps the verticals pages
 *
 * @author gof
 *
 */
public class VerticalController  extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalController.class);
	private  final UiService uiService;
	private final VerticalsConfigService verticalService;
	private final SearchService searchService;
	private final BlogService blogService;

	private String vertical;

	public VerticalController( VerticalsConfigService verticalService, SearchService searchService, UiService uiService, String vertical, BlogService blogService) {
		this.verticalService = verticalService;
		this.searchService = searchService;
		this.uiService = uiService;
		this.vertical = vertical;
		this.blogService = blogService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView ret = uiService.defaultModelAndView(("vertical-home"), request);


		VerticalConfig config = verticalService.getConfigById(this.vertical);

		// TODO : strategy of injection of products for nativ SEO

		VerticalSearchRequest vRequest = new VerticalSearchRequest();
		vRequest.setSortField("scores.ECOSCORE.value");
		vRequest.setSortOrder("desc");
		vRequest.getNumericFilters().add(new NumericRangeFilter("offersCount", 1.0, 10000.0, 1.0, false));
		vRequest.getNumericFilters().add(new NumericRangeFilter("price.minPrice.price", 0.0001, 500000.0, 100.0, false));
		vRequest.getNumericFilters().add(new NumericRangeFilter("scores.ECOSCORE.value", 0.0001, 500000.0, 0.1, false));
		
		
		List<AttributeConfig> numericFilters = config.getVerticalFilters().stream()
			.map(e -> config.getAttributesConfig().getAttributeConfigByKey(e))
			.filter(e-> e != null)
			.filter(e -> e.getFilteringType() == AttributeType.NUMERIC).toList();
		
		numericFilters.forEach(filter -> {
			vRequest.getNumericFilters().add(new NumericRangeFilter("attributes.indexed."+ filter.getKey()+".numericValue", 0.0, 1000000.0, 1.0, true));
		});
		
		
		VerticalSearchResponse vResponse = searchService.verticalSearch(config,vRequest);
		
		Map<String,String> countryNames = new HashMap<>();
		for (VerticalFilterTerm country : vResponse.getCountries()) {
			countryNames.put(country.getText(), new ULocale("",country.getText()).getDisplayCountry( new ULocale(request.getLocale().toString())));
		}

		ret.addObject("countryNames", countryNames);
		ret.addObject("products", vResponse);
		
		ret.addObject("posts",blogService.getPosts(vertical));
		ret.addObject("verticalConfig",config);

		ret.addObject("vertical",this.vertical);
		ret.addObject("verticals",verticalService.getConfigsWithoutDefault());
		
		
		// TODO: i18n
		ret.addObject("verticalPath",verticalService.getPathForVerticalLanguage("fr",config));

		return ret;
	}

}
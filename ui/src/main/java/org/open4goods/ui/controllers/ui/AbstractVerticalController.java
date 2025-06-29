package org.open4goods.ui.controllers.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.commons.model.dto.NumericRangeFilter;
import org.open4goods.commons.model.dto.VerticalFilterTerm;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.services.blog.service.BlogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.ibm.icu.util.ULocale;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This controller maps the verticals pages
 *
 * @author gof
 *
 */
public abstract class AbstractVerticalController  extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVerticalController.class);
	protected final UiService uiService;
	protected final VerticalsConfigService verticalService;
	protected final SearchService searchService;
	protected final BlogService blogService;

	protected String vertical;
	protected SerialisationService serialisationService;

	public AbstractVerticalController( VerticalsConfigService verticalService, SearchService searchService, UiService uiService, String vertical, BlogService blogService, SerialisationService serialisationService) {
		this.verticalService = verticalService;
		this.searchService = searchService;
		this.uiService = uiService;
		this.vertical = vertical;
		this.blogService = blogService;
		this.serialisationService = serialisationService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////




	protected void completeResponse(HttpServletRequest request, ModelAndView ret, VerticalConfig config, VerticalSearchResponse vResponse) {
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
	}

	// TODO(p2, design) : Externalize in a service
	public static VerticalSearchRequest buildDefaultRequest(ModelAndView ret, VerticalConfig config) {
		VerticalSearchRequest vRequest = new VerticalSearchRequest();
		
		// We show excluded products only to logged in people
		if (ret.getModel().containsKey("user")) {
			vRequest.setExcluded(true);
		}
		
		vRequest.setSortField("scores.ECOSCORE.value");
		vRequest.setSortOrder("desc");
		vRequest.getNumericFilters().add(new NumericRangeFilter("offersCount", 1.0, 10000.0, 1.0, false));
		vRequest.getNumericFilters().add(new NumericRangeFilter("price.minPrice.price", 0.0001, 500000.0, 100.0, false));
		vRequest.getNumericFilters().add(new NumericRangeFilter("scores.ECOSCORE.value", 0.0001, 500000.0, 0.1, true));
		vRequest.getNumericFilters().add(new NumericRangeFilter("scores.BRAND_SUSTAINABILITY.value", 0.0001, 500000.0, 0.1, true));
		vRequest.getNumericFilters().add(new NumericRangeFilter("attributes.indexed.POWER_CONSUMPTION.numericValue", 0.0001, 500000.0, 0.1, true));
		vRequest.getNumericFilters().add(new NumericRangeFilter("attributes.indexed.POWER_CONSUMPTION_OFF.numericValue", 0.0001, 500000.0, 0.1, true));
		
		
		List<AttributeConfig> numericFilters = config.getVerticalFilters().stream()
			.map(e -> config.getAttributesConfig().getAttributeConfigByKey(e))
			.filter(e-> e != null)
			.filter(e -> e.getFilteringType() == AttributeType.NUMERIC).toList();
		
		numericFilters.forEach(filter -> {
			vRequest.getNumericFilters().add(new NumericRangeFilter("attributes.indexed."+ filter.getKey()+".numericValue", 0.0, 1000000.0, 1.0, true));
		});
		return vRequest;
	}

}
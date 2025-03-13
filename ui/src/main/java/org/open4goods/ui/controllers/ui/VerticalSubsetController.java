package org.open4goods.ui.controllers.ui;

import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.services.BlogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller maps the classical verticals pages
 *
 * @author gof
 *
 */
public class VerticalSubsetController  extends AbstractVerticalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalSubsetController.class);

	private VerticalSubset subset;
	public VerticalSubsetController( VerticalsConfigService verticalService, SearchService searchService, UiService uiService, String vertical, BlogService blogService, SerialisationService serialisationService, VerticalSubset subset) {
		super(verticalService, searchService, uiService, vertical, blogService, serialisationService);
		this.subset = subset;
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

		VerticalSearchRequest vRequest = buildDefaultRequest(ret, config);
		vRequest.getSubsets().add(subset);
		
		VerticalSearchResponse vResponse = searchService.verticalSearch(config,vRequest);
		
		
		
		
		completeResponse(request, ret, config, vResponse);
		ret.addObject("subset",subset);

		return ret;
	}

}
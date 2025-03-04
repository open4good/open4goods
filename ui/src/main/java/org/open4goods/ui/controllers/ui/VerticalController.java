package org.open4goods.ui.controllers.ui;

import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.serialisation.service.SerialisationService;
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
public class VerticalController  extends AbstractVerticalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalController.class);

	public VerticalController( VerticalsConfigService verticalService, SearchService searchService, UiService uiService, String vertical, BlogService blogService, SerialisationService serialisationService) {
		super(verticalService, searchService, uiService, vertical, blogService, serialisationService);
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
		
		VerticalSearchResponse vResponse = searchService.verticalSearch(config,vRequest);
		
		completeResponse(request, ret, config, vResponse);

		return ret;
	}

}
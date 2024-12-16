package org.open4goods.ui.config;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.services.BlogService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class VerticalEcoscoreController extends AbstractController  {

	private VerticalConfig verticalConfig;
	private UiService uiService;

	public VerticalEcoscoreController(VerticalConfig verticalConfig, UiService uiService) {
		this.verticalConfig = verticalConfig;
		this.uiService = uiService;
		
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView ret = uiService.defaultModelAndView(("ecoscore-vertical"), request);
		
		ret.addObject("impactscore",verticalConfig.getImpactScoreConfig());
		ret.addObject("verticalConfig",verticalConfig);
		return ret;

	}

}

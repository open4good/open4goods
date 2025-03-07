package org.open4goods.ui.config;

import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.services.BlogService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class VerticalEcoscoreController extends AbstractController  {

	private VerticalConfig verticalConfig;
	private UiService uiService;
	private SerialisationService serialisationService;

	public VerticalEcoscoreController(VerticalConfig verticalConfig, UiService uiService, SerialisationService serialisationService) {
		this.verticalConfig = verticalConfig;
		this.uiService = uiService;
		this.serialisationService = serialisationService;
		
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView ret = uiService.defaultModelAndView(("ecoscore-vertical"), request);
		
		ret.addObject("impactscore",verticalConfig.getImpactScoreConfig());
		
		// TODO(p2,perf) : cache
		PromptConfig prompt = serialisationService.fromYaml(verticalConfig.getImpactScoreConfig().getYamlPrompt(), PromptConfig.class);
		
		
		ret.addObject("prompt",prompt);
		ret.addObject("verticalConfig",verticalConfig);

		return ret;

	}

}

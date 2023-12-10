package org.open4goods.ui.controllers.ui;

import org.open4goods.dao.ProductRepository;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.services.OpenDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

	private final ProductRepository aggregatedDataRepository;
	private final DataSourceConfigService datasourceConfigService;

	private final VerticalsConfigService verticalConfigService;
	private OpenDataService openDataService;

	public HomeController(ProductRepository aggregatedDataRepository, DataSourceConfigService datasourceConfigService, VerticalsConfigService verticalConfigService) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.datasourceConfigService = datasourceConfigService;
		this.verticalConfigService = verticalConfigService;
	}


	@GetMapping("/")
	public ModelAndView index(final HttpServletRequest request) {

		// TODO : Remove this test page
		ModelAndView model = defaultModelAndView("index", request);

		model.addObject("totalItems", aggregatedDataRepository.countMainIndexHavingPrice());

		// TODO(gof) : deduplicate (darty.com / darty.com-CSV)
		model.addObject("partners",  datasourceConfigService.datasourceConfigs().size());

		model.addObject("verticals",  verticalConfigService.getConfigsWithoutDefault());

		model.addObject("url",  "/");

		return model;
	}


}

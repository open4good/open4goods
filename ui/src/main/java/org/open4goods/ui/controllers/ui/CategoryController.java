package org.open4goods.ui.controllers.ui;

import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller maps the verticals pages
 *
 * @author gof
 *
 */
public class CategoryController  extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CategoryController.class);
	private  final UiService uiService;
	private ProductCategory category;
	private GoogleTaxonomyService googleService;

	public CategoryController( UiService uiService, ProductCategory productCategory, GoogleTaxonomyService googleService) {
		this.uiService = uiService;
		this.category = productCategory;
		this.googleService = googleService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView ret = uiService.defaultModelAndView(("category"), request);

		ret.addObject("category", category);
		ret.addObject("havingVertical",true);
		ret.addObject("googleProductService", googleService);
		return ret;
	}

}
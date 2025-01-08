package org.open4goods.ui.controllers.ui;

import java.net.URLDecoder;

import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.open4goods.commons.config.yml.ui.SubsetCriteria;
import org.open4goods.commons.config.yml.ui.SubsetCriteriaOperator;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.config.yml.ui.VerticalSubset;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.VerticalsConfigService;
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
public class VerticalBrandsController  extends AbstractVerticalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalBrandsController.class);

	public VerticalBrandsController( VerticalsConfigService verticalService, SearchService searchService, UiService uiService, String vertical, BlogService blogService, SerialisationService serialisationService) {
		super(verticalService, searchService, uiService, vertical, blogService, serialisationService);
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView ret = uiService.defaultModelAndView(("vertical-home"), request);

		String brand = request.getServletPath().substring(request.getServletPath().lastIndexOf('/')+1);
		
		if (StringUtils.isEmpty(brand)) {
			// TODO :(p2, design) : Throw invalid parameter / 404
			return null;
		}
		
		brand = URLDecoder.decode(brand,"UTF-8").toUpperCase();

		VerticalConfig config = verticalService.getConfigById(this.vertical);

		// TODO : strategy of injection of products for nativ SEO

		VerticalSearchRequest vRequest = buildDefaultRequest(ret, config);
		
		
		// Get the default subset, to have texts and so on
		VerticalSubset brandSubset = serialisationService.clone(config.getBrandsSubset());
		brandSubset.getCriterias().add(new SubsetCriteria("attributes.referentielAttributes.BRAND",SubsetCriteriaOperator.EQUALS, brand));
		vRequest.setBrandsSubset(brandSubset);
		
		// Searching the products
		VerticalSearchResponse vResponse = searchService.verticalSearch(config,vRequest);
		
		// Complete the view with standards verticals attributes
		completeResponse(request, ret, config, vResponse);

		return ret;
	}

}
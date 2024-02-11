package org.open4goods.ui.controllers.ui;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.dto.WikiResult;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.services.ai.AiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller

/**
 * This controller pages pageSize Xwiki content
 *
 * @author gof
 *
 */
public class AdminController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

	// The siteConfig
	private final UiConfig config;

	@Autowired
	private  VerticalsConfigService configService;
	
	private final VerticalsConfigService verticalService;
	
	private final AiService aiService;


	private  ProductRepository repository;
	
	
	public AdminController(UiConfig config, VerticalsConfigService verticalsConfigService, AiService aiService, ProductRepository repository) {
		this.config = config;
		this.verticalService = verticalsConfigService;
		this.aiService = aiService;
		this.repository = repository;
	}


	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////


	/**
	 * reload verticals config 
	 * @param request
	 * @return 
	 */
	@GetMapping("/reloadConfigs")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView reloadConfigs(final HttpServletRequest request, @RequestParam(name = "r", required = false) String redircectUrl) {
		verticalService.loadConfigs();
		ModelAndView mv = null;
		if (null != redircectUrl) {
			 mv = new ModelAndView("redirect:"+ redircectUrl);
			mv.setStatus(HttpStatus.MOVED_TEMPORARILY);				
		}
		return mv;
	}

	/**
	 * reload verticals config 
	 * @param request
	 * @return 
	 * @throws ResourceNotFoundException 
	 */
	@GetMapping("/aiAssist")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView aiAssist(final HttpServletRequest request, @RequestParam(name = "r", required = false) String redircectUrl,  @RequestParam(name = "gtin", required = false) String gtin) throws ResourceNotFoundException {
		
		Product data = repository.getById(gtin);
		
		aiService.complete(data, configService.getConfigByIdOrDefault(data.getVertical()));
		
		repository.forceIndex(data);

	
		
		ModelAndView mv = null;
		if (null != redircectUrl) {
			 mv = new ModelAndView("redirect:"+ redircectUrl);
			mv.setStatus(HttpStatus.MOVED_TEMPORARILY);				
		}
		return mv;
	}

}
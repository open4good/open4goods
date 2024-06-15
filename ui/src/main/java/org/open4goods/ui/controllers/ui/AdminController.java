package org.open4goods.ui.controllers.ui;

import java.io.IOException;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.product.Product;
import org.open4goods.services.ImageGenerationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.ai.AiService;
//import org.open4goods.services.ai.AiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.SitemapGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller

/**
 * This controller pages pageSize Xwiki content
 *
 * @author gof
 *
 */
public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	// The siteConfig
	private final UiConfig config;


	private final VerticalsConfigService verticalService;

	private final SitemapGenerationService sitemapService;

	private final AiService aiService;

	private ProductRepository repository;

	private ImageGenerationService imageGenerationService;

	private VerticalsConfigService verticalsConfigService;

	public AdminController(UiConfig config, VerticalsConfigService verticalsConfigService,
			ProductRepository repository, SitemapGenerationService sitemapService, ImageGenerationService imageGenerationService, AiService aiService) {
		this.config = config;
		this.verticalService = verticalsConfigService;
		this.sitemapService = sitemapService;
		this.repository = repository;
		this.imageGenerationService = imageGenerationService;
		this.verticalsConfigService = verticalsConfigService;
		this.aiService = aiService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * reload verticals config
	 * 
	 * @param request
	 * @return
	 */
	@GetMapping("/reloadConfigs")
	@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
	public ModelAndView reloadConfigs(final HttpServletRequest request,
			@RequestParam(name = "r", required = false) String redircectUrl) {
		verticalService.loadConfigs();
		ModelAndView mv = null;
		if (null != redircectUrl) {
			mv = new ModelAndView("redirect:" + redircectUrl);
			mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		}
		return mv;
	}

	/**
	 * reload verticals config
	 *
	 * @param request
	 * @return
	 * @throws ResourceNotFoundException
	 */
	@GetMapping("/aiAssist")
	@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
	public ModelAndView aiAssist(final HttpServletRequest request,
			@RequestParam(name = "r", required = false) String redircectUrl,
			@RequestParam(name = "gtin", required = false) String gtin) throws ResourceNotFoundException {

		Product data = repository.getById(gtin);

		// We always force when human triggered
		aiService.complete(data, verticalsConfigService.getConfigByIdOrDefault(data.getVertical()),true);

		repository.forceIndex(data);

		ModelAndView mv = null;
		if (null != redircectUrl) {
			mv = new ModelAndView("redirect:" + redircectUrl);
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
	@GetMapping("/sitemap")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView sitemap(final HttpServletRequest request) throws ResourceNotFoundException {
		
		sitemapService.generate();
		ModelAndView mv = new ModelAndView("redirect:/");
		mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		return mv;
	}

//	@GetMapping("/reloadVerticalImage/{verticalId}")
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
//	public ModelAndView reloadVerticalImage (@PathVariable(name= "verticalId") String verticalId) {
//	imageGenerationService.generateImage(verticalId);
//
//
//	}

	@GetMapping("/regenerate/{verticalId}")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView regenerateVerticalImage(@PathVariable String verticalId) throws IOException {
		VerticalConfig verticalConfig = verticalsConfigService.getConfigById(verticalId);
		String verticalTitle = verticalConfig.getI18n().get("default").getVerticalHomeTitle();
		String fileName = verticalId + ".png";
		imageGenerationService.fullGenerate(verticalTitle, fileName);
		logger.info("Image for vertical {} with title '{}' has been regenerated", verticalId, verticalTitle);
		
		ModelAndView mv = new ModelAndView("redirect:/");
		mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		return mv;
	}

}
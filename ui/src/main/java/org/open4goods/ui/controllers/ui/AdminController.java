package org.open4goods.ui.controllers.ui;

import java.io.IOException;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
//import org.open4goods.services.ai.AiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.GoogleIndexationService;
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

	private ProductRepository repository;


	private VerticalsConfigService verticalsConfigService;

	private GoogleIndexationService googleIndexationService;

	public AdminController(UiConfig config, VerticalsConfigService verticalsConfigService,
			ProductRepository repository, SitemapGenerationService sitemapService) {
		this.config = config;
		this.verticalService = verticalsConfigService;
		this.sitemapService = sitemapService;
		this.repository = repository;
		this.verticalsConfigService = verticalsConfigService;
		this.googleIndexationService = googleIndexationService;
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



//		String verticalTitle = verticalConfig.getI18n().get("default").getVerticalHomeTitle();
//		String fileName = verticalId + ".png";
//		imageGenerationService.fullGenerate(verticalTitle, fileName);
//		logger.info("Image for vertical {} with title '{}' has been regenerated", verticalId, verticalTitle);
//
		ModelAndView mv = new ModelAndView("redirect:/");
		mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		return mv;
	}



	@GetMapping("/index/{verticalId}")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView index(@PathVariable String verticalId, HttpServletRequest request) throws IOException {

		// TODO(p3,i18n) : Should be siteLocale
		googleIndexationService.indexVertical(verticalId, config.getBaseUrl(request.getLocale()));

		ModelAndView mv = new ModelAndView("redirect:/");
		mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		return mv;
	}

	@GetMapping("/index")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView indexPage(HttpServletRequest request, @RequestParam(name = "r", required = false) String redircectUrl) throws IOException {

		googleIndexationService.indexPage(redircectUrl);

		ModelAndView mv = new ModelAndView("redirect:/");
		mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		return mv;
	}

	@GetMapping("/indexNew")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ModelAndView indexNew(HttpServletRequest request ) throws IOException {

		googleIndexationService.indexNewProducts();

		ModelAndView mv = new ModelAndView("redirect:/");
		mv.setStatus(HttpStatus.MOVED_TEMPORARILY);
		return mv;
	}




}
package org.open4goods.ui.controllers.ui;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.productrepository.services.ProductRepository;
//import org.open4goods.services.ai.AiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.SitemapGenerationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
public class SitemapController {

	private static final Logger logger = LoggerFactory.getLogger(SitemapController.class);


	private final SitemapGenerationService sitemapService;



	public SitemapController(
			 SitemapGenerationService sitemapService) {
		this.sitemapService = sitemapService;
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





}
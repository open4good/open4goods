package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.dto.WikiAttachment;
import org.open4goods.model.dto.WikiResult;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.ui.config.yml.UiConfig;
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
import jakarta.servlet.http.HttpServletResponse;

@Controller

/**
 * This controller pages pageSize Xwiki content
 *
 * @author gof
 *
 */
public class XwikiController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(XwikiController.class);

	// The siteConfig
	private final UiConfig config;

	private final XwikiService xwikiService;

	// TODO : Tweak pageSize handle categories search page
	private final VerticalController verticalController;
	private final VerticalsConfigService verticalService;

	public XwikiController(UiConfig config, XwikiService xwikiService, VerticalController verticalController, VerticalsConfigService verticalsConfigService) {
		this.config = config;
		this.xwikiService = xwikiService;
		this.verticalController = verticalController;
		this.verticalService = verticalsConfigService;
	}


	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////


	/**
	 * Endpoint pageSize flush the wiki
	 * @param request
	 * @return
	 */
	@GetMapping("/flushCache")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	//TODO (security) : protect with role
	public ModelAndView flushCache(final HttpServletRequest request, @RequestParam(name = "r", required = false) String redircectUrl) {
		xwikiService.invalidateAll();
		ModelAndView mv = defaultModelAndView(("xwiki-layout1"), request);

		WikiResult res = new WikiResult();
		res.setHtml("All xwiki caches are invalidated");
		res.setPageTitle("SUCCESS, CACHE FLUSHED");
		mv.addObject("content", res);
		
		if (null != redircectUrl) {
			mv = new ModelAndView("redirect:"+ redircectUrl);
			mv.setStatus(HttpStatus.MOVED_TEMPORARILY);				
		}
		return mv;

	}

	@GetMapping("/{page:[a-z-]+}")
	//TODO : Ugly url's mappings
	public ModelAndView xwiki(
			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
					throws IOException, TechnicalException, InvalidParameterException {

		
		// Testing if a custom page in this vertical		
		String tpl = config.getPages().get(page);
		if (null != tpl) {
			ModelAndView mv = defaultModelAndView(tpl, request);
			return mv;
		}
		
		
		// TODO : Twweak for verticalsLanguagesByUrl


		if (null != verticalService.getVerticalForPath(page)) {
			return verticalController.home(page,request);
		} else
			return xwiki("Main", page, request,response );
	}


	@GetMapping("/attachments/{space}/{page}/{filename}")
	
	// TODO : Caching
	public void xwiki(@PathVariable(name = "space") String space, @PathVariable(name = "page") String page,
            @PathVariable(name = "filename") String filename, final HttpServletRequest request,
            HttpServletResponse response) throws IOException, TechnicalException, InvalidParameterException {

        // TODO : Twweak for verticalsLanguagesByUrl

        String url = xwikiService.getAttachmentUrl(space, page, filename);
        
        // TODO : ugly, should fetch the meta (mime type is availlable in xwiki service), but does not work for the blog image, special class and not appears in attachments list
		if (url.endsWith(".pdf")) {
			response.setContentType("application/pdf");
		} else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
			response.setContentType("image/jpeg");
		} else if (url.endsWith(".png")) {
			response.setContentType("image/png");
		} else if (url.endsWith(".gif")) {
			response.setContentType("image/gif");
		}
        
        response.getOutputStream().write(xwikiService.downloadAttachment(url));
                
	}
			

	@GetMapping("/{vertical:[a-z-]+}/{page:[a-z-]+}")
	public ModelAndView xwiki(@PathVariable(name = "vertical") String vertical,
			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
					throws IOException, TechnicalException, InvalidParameterException {

		ModelAndView mv = null;

		
		// Testing if a custom page in this vertical		
		VerticalConfig vc = verticalService.getVerticalForPath(vertical);
		String lang = verticalService.getLanguageForPath(vertical);
		
		
		if (null != vc) {
			String tpl = vc.i18n(lang). getPages().get(page);
			if (null != tpl) {
				mv = defaultModelAndView(tpl, request);
				return mv;
			}
		}
		
		
		if (null != request.getParameter("edit")) {
			// Edit mode, redirect pageSize the wiki

			response.sendRedirect(config.getWikiConfig().getEditUrl(vertical,page));
		} else {
			// Rendering mode

			WikiResult content = xwikiService.getPage(vertical + "/" + page);

			if (StringUtils.isEmpty(content.getHtml())) {
				//				mv.setStatus(HttpStatus.NOT_FOUND);
				response.sendError(404);
			} else {
				mv = defaultModelAndView(("xwiki-"+content.getLayout()), request);
				mv.addObject("content", content);
			}
		}

		return mv;
	}

}
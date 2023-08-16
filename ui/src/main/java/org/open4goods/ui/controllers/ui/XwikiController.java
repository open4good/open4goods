package org.open4goods.ui.controllers.ui;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.dto.WikiResult;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private @Autowired UiConfig config;

	private @Autowired XwikiService xwikiService;

	// TODO : Tweak pageSize handle categories search page
	private @Autowired VerticalController verticalController;
	private @Autowired VerticalsConfigService verticalService;






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
	public ModelAndView flushCache(final HttpServletRequest request) {
		xwikiService.invalidateAll();
		ModelAndView mv = defaultModelAndView(("xwiki-layout1"), request);

		WikiResult res = new WikiResult();
		res.setHtml("All xwiki caches are invalidated");
		res.setPageTitle("SUCCESS, CACHE FLUSHED");

		mv.addObject("content", res);

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




	@GetMapping("/{vertical:[a-z-]+}/{page:[a-z-]+}")
	public ModelAndView xwiki(@PathVariable(name = "vertical") String vertical,
			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
					throws IOException, TechnicalException, InvalidParameterException {

		ModelAndView mv = null;

		
		// Testing if a custom page in this vertical		
		VerticalConfig vc = verticalService.getVerticalForPath(vertical);
		if (null != vc) {
			String tpl = vc.getPages().get(page);
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
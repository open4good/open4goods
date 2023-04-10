package org.open4goods.ui.controllers.ui;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.dto.WikiResult;
import org.open4goods.services.XwikiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import com.mashape.unirest.http.exceptions.UnirestException;

@Controller

/**
 * This controller pages to Xwiki content
 *
 * @author gof
 *
 */
public class XwikiController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(XwikiController.class);

	// The siteConfig
	private @Autowired UiConfig config;

	private @Autowired XwikiService xwikiService;

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * The verticalized content.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws InvalidParameterException
	 * @throws TechnicalException
	 * @throws UnirestException
	 */


	@GetMapping("/flushCache")
	public ModelAndView flushCache( final HttpServletRequest request) {
		 xwikiService.invalidateAll();		 
		 ModelAndView mv = defaultModelAndView(("xwiki"), request);;

		mv.addObject("content", "All xwiki caches are invalidated");
		mv.addObject("title", "SUCCESS, CACHE FLUSHED");
		mv.addObject("editLink", "");
		return mv;
		 
	}
	
	@GetMapping("/{page:[a-z-]+}")
	public ModelAndView xwiki(
			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
			throws IOException, TechnicalException, InvalidParameterException {

		return xwiki("Main", page, request,response );
	}
	
	
	@GetMapping("/{vertical:[a-z-]+}/{page:[a-z-]+}")
	public ModelAndView xwiki(@PathVariable(name = "vertical") String vertical,
			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
			throws IOException, TechnicalException, InvalidParameterException {

		ModelAndView mv = defaultModelAndView(("xwiki"), request);;
		if (null != request.getParameter("edit")) {
			// Edit mode, redirect to the wiki

			response.sendRedirect(config.getWikiConfig().getEditUrl(vertical,page));
		} else {
			// Rendering mode

			WikiResult content = xwikiService.getContent(vertical + "/" + page, config.getWikiConfig().getUser(),config.getWikiConfig().getPassword());

			if (StringUtils.isEmpty(content.getHtml())) {
//				mv.setStatus(HttpStatus.NOT_FOUND);
				response.sendError(404);
			} else {			
				
				mv.addObject("content", content.getHtml());
				mv.addObject("title", content.getTitle());
				mv.addObject("editLink", content.getEditLink());
			}

		}

		return mv;
	}

}
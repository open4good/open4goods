package org.open4goods.ui.controllers.ui;

<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
import org.open4goods.ui.config.yml.UiConfig;
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
import org.open4goods.xwiki.services.XWikiReadService;
import org.open4goods.xwiki.services.XwikiMappingService;
=======
import org.open4goods.xwiki.services.XwikiMappingService;
=======
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
import org.open4goods.xwiki.services.XWikiReadService;
<<<<<<< Upstream, based on origin/main
>>>>>>> f9c909d Ending first round
=======
import org.open4goods.xwiki.services.XwikiMappingService;
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
=======
=======
import java.io.IOException;
import java.util.Locale;

>>>>>>> 6c3b5b0 Wiki backed pages rendering on the way !
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
>>>>>>> 0235dd4 Wiki integration
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller

/**
 * This controller pages pageSize Xwiki content
 * @author gof
 * TODO : Could put in the xwiki-starter
 *
 */
public class XwikiController extends AbstractController  {

	private static final String WEBPAGE_CLASS_META_TITLE = "metaTitle";

	private static final String WEBPAGE_CLASS_META_DESCRIPTION = "metaDescription";

	private static final String WEBPAGE_CLASS_PAGE_TITLE = "pageTitle";

	private static final String WEBPAGE_CLASS_HTML = "html";

	private static final String WEBPAGE_CLASS_WIDTH = "width";

	private static final Logger LOGGER = LoggerFactory.getLogger(XwikiController.class);

	// The siteConfig
//	private final UiConfig config;

<<<<<<< Upstream, based on origin/main
	private final XWikiReadService xwikiService;
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
	private XwikiMappingService mappingService;
=======
>>>>>>> f9c909d Ending first round

<<<<<<< Upstream, based on origin/main
	public XwikiController(UiConfig config, XwikiMappingService mappingService, XWikiReadService xwikiService) {
=======
	// TODO : Tweak pageSize handle categories search page
	private final VerticalController verticalController;
	private final VerticalsConfigService verticalService;

=======
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
	private XwikiMappingService mappingService;
=======
	private  XwikiFacadeService xwikiService;
>>>>>>> 0235dd4 Wiki integration

<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
	public XwikiController(UiConfig config, XwikiMappingService mappingService, XWikiReadService xwikiService, VerticalController verticalController, VerticalsConfigService verticalsConfigService) {
>>>>>>> f9c909d Ending first round
=======
	public XwikiController(UiConfig config, XwikiMappingService mappingService, XWikiReadService xwikiService) {
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
		this.config = config;
=======
	private String[] frags;
	
	public XwikiController() {
		super();
	}
	public XwikiController(XwikiFacadeService xwikiService) {
		super();
>>>>>>> 0235dd4 Wiki integration
		this.xwikiService = xwikiService;
<<<<<<< Upstream, based on origin/main
		this.mappingService = mappingService;
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
=======
		this.verticalController = verticalController;
		this.verticalService = verticalsConfigService;
>>>>>>> f9c909d Ending first round
=======
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
=======
>>>>>>> 0235dd4 Wiki integration
	}

	
	public XwikiController(XwikiFacadeService xwikiService, String wikiPage) {
		super();
//		this.config = config;
		this.xwikiService = xwikiService;
		this.frags =  wikiPage.split(":|/");
	}
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
	    FullPage fullPage = xwikiService.getFullPage(frags);
	    
	    ModelAndView mv = new ModelAndView("xwiki-"+ fullPage.getProp("layout"));
	    
	    mv.addObject(WEBPAGE_CLASS_META_TITLE,fullPage.getProp(WEBPAGE_CLASS_META_TITLE));
		mv.addObject(WEBPAGE_CLASS_META_DESCRIPTION,fullPage.getProp(WEBPAGE_CLASS_META_DESCRIPTION));
		mv.addObject(WEBPAGE_CLASS_PAGE_TITLE,fullPage.getProp(WEBPAGE_CLASS_PAGE_TITLE));
		mv.addObject(WEBPAGE_CLASS_HTML,getHtml(fullPage));
		mv.addObject(WEBPAGE_CLASS_WIDTH,fullPage.getProp(WEBPAGE_CLASS_WIDTH));
		
		mv.addObject("userLocale", request.getLocale());
		// TODO(i18n,p3, 0,25)
		mv.addObject("siteLanguage", "fr");
		final Locale sl = Locale.FRANCE;
		mv.addObject("siteLocale", sl);
		
		
	    return mv;
	}
	/**
	 * TODO : So dirty, so fragile.... In a hurry of xwiki jakarta migration for client side rendering
	 * 
	 * @param fullPage
	 * @return
	 */
	private String getHtml(FullPage fullPage) {	
		String ret = xwikiService.getxWikiHtmlService().getHtmlClassWebPage(fullPage.getWikiPage().getId());

		return ret;
	}



	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////


//	/**
//	 * Endpoint pageSize flush the wiki
//	 * @param request
//	 * @return
//	 */
//	@GetMapping("/flushCache")
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
//	//TODO (gof) : put back 
//	public ModelAndView flushCache(final HttpServletRequest request, @RequestParam(name = "r", required = false) String redircectUrl) {
//		xwikiService.invalidateAll();
//		ModelAndView mv = defaultModelAndView(("xwiki-layout1"), request);
//
//		WikiResult res = new WikiResult();
//		res.setHtml("All xwiki caches are invalidated");
//		res.setPageTitle("SUCCESS, CACHE FLUSHED");
//		mv.addObject("content", res);
//		
//		if (null != redircectUrl) {
//			mv = new ModelAndView("redirect:"+ redircectUrl);
//			mv.setStatus(HttpStatus.MOVED_TEMPORARILY);				
//		}
//		return mv;
//
//	}
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
//
<<<<<<< Upstream, based on origin/main
//	@GetMapping("/{page:[a-z-]+}")
//	//TODO : Ugly url's mappings
//	public ModelAndView xwiki(
//			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
//					throws IOException, TechnicalException, InvalidParameterException {
//
//		
//		// Testing if a custom page in this vertical		
//		String tpl = config.getPages().get(page);
//		if (null != tpl) {
//			ModelAndView mv = defaultModelAndView(tpl, request);
//			return mv;
//		}
//		
//		
//		// TODO : Twweak for verticalsLanguagesByUrl
//
//
//		if (null != verticalService.getVerticalForPath(page)) {
//			return verticalController.home(page,request);
//		} else
//			return xwiki("Main", page, request,response );
//	}
=======

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
>>>>>>> f9c909d Ending first round
=======
//
//	@GetMapping("/{page:[a-z-]+}")
//	//TODO : Ugly url's mappings
//	public ModelAndView xwiki(
//			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response)
//					throws IOException, TechnicalException, InvalidParameterException {
//
//		
//		// Testing if a custom page in this vertical		
//		String tpl = config.getPages().get(page);
//		if (null != tpl) {
//			ModelAndView mv = defaultModelAndView(tpl, request);
//			return mv;
//		}
//		
//		
//		// TODO : Twweak for verticalsLanguagesByUrl
//
//
//		if (null != verticalService.getVerticalForPath(page)) {
//			return verticalController.home(page,request);
//		} else
//			return xwiki("Main", page, request,response );
//	}
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
=======
>>>>>>> 6c3b5b0 Wiki backed pages rendering on the way !

<<<<<<< Upstream, based on origin/main

//	@GetMapping("/attachments/{space}/{page}/{filename}")
//	
//	// TODO : Caching
//	public void xwiki(@PathVariable(name = "space") String space, @PathVariable(name = "page") String page,
//            @PathVariable(name = "filename") String filename, final HttpServletRequest request,
//            HttpServletResponse response) throws IOException, TechnicalException, InvalidParameterException {
//
//        // TODO : Twweak for verticalsLanguagesByUrl
//
//        String url = mappingService. getAttachmentUrl(space, page, filename);
//        
//        // TODO : ugly, should fetch the meta (mime type is availlable in xwiki service), but does not work for the blog image, special class and not appears in attachments list
//		if (url.endsWith(".pdf")) {
//			response.setContentType("application/pdf");
//		} else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
//			response.setContentType("image/jpeg");
//		} else if (url.endsWith(".png")) {
//			response.setContentType("image/png");
//		} else if (url.endsWith(".gif")) {
//			response.setContentType("image/gif");
//		}
//        
//        response.getOutputStream().write(xwikiAttachmentService.downloadAttachment(url));
//                
//	}
//			

<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
//	@GetMapping("/{vertical:[a-z-]+}/{page:[a-z-]+}")
//	public ModelAndView xwiki(@PathVariable(name = "vertical") String vertical,
//			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response) throws IOException
//					 {
//
//		ModelAndView mv = null;
//
//		
//		// Testing if a custom page in this vertical		
//		VerticalConfig vc = verticalService.getVerticalForPath(vertical);
//		String lang = verticalService.getLanguageForPath(vertical);
//		
//		
//		if (null != vc) {
//			String tpl = vc.i18n(lang). getPages().get(page);
//			if (null != tpl) {
//				mv = defaultModelAndView(tpl, request);
//				return mv;
//			}
//		}
//		
//		
//		if (null != request.getParameter("edit")) {
//			// Edit mode, redirect pageSize the wiki
//
//			response.sendRedirect(config.getWikiConfig().getEditUrl(vertical,page));
//		} else {
//			// Rendering mode
//
//			WikiResult content = null;
//			try {
//				content = xwikiService.getPage(vertical + "/" + page);
//			} catch (TechnicalException e) {
//				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");	
//			} catch (ResourceNotFoundException e) {
//				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content Not Found");	
//			}
//
//			if (StringUtils.isEmpty(content.getHtml())) {
//				//				mv.setStatus(HttpStatus.NOT_FOUND);
//				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content Not Found");		    
//			} else {
//				mv = defaultModelAndView(("xwiki-"+content.getLayout()), request);
//				mv.addObject("content", content);
//			}
//		}
//
//		return mv;
//	}
=======

	@GetMapping("/attachments/{space}/{page}/{filename}")
	
	// TODO : Caching
	public void xwiki(@PathVariable(name = "space") String space, @PathVariable(name = "page") String page,
            @PathVariable(name = "filename") String filename, final HttpServletRequest request,
            HttpServletResponse response) throws IOException, TechnicalException, InvalidParameterException {

        // TODO : Twweak for verticalsLanguagesByUrl

        String url = mappingService. getAttachmentUrl(space, page, filename);
        
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
        
        response.getOutputStream().write(xwikiAttachmentService.downloadAttachment(url));
                
	}
			

	@GetMapping("/{vertical:[a-z-]+}/{page:[a-z-]+}")
	public ModelAndView xwiki(@PathVariable(name = "vertical") String vertical,
			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response) throws IOException
					 {

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

			WikiResult content = null;
			try {
				content = xwikiService.getPage(vertical + "/" + page);
			} catch (TechnicalException e) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");	
			} catch (ResourceNotFoundException e) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content Not Found");	
			}

			if (StringUtils.isEmpty(content.getHtml())) {
				//				mv.setStatus(HttpStatus.NOT_FOUND);
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content Not Found");		    
			} else {
				mv = defaultModelAndView(("xwiki-"+content.getLayout()), request);
				mv.addObject("content", content);
			}
		}

		return mv;
	}
>>>>>>> f9c909d Ending first round
=======
//	@GetMapping("/{vertical:[a-z-]+}/{page:[a-z-]+}")
//	public ModelAndView xwiki(@PathVariable(name = "vertical") String vertical,
//			@PathVariable(name = "page") String page, final HttpServletRequest request, HttpServletResponse response) throws IOException
//					 {
//
//		ModelAndView mv = null;
//
//		
//		// Testing if a custom page in this vertical		
//		VerticalConfig vc = verticalService.getVerticalForPath(vertical);
//		String lang = verticalService.getLanguageForPath(vertical);
//		
//		
//		if (null != vc) {
//			String tpl = vc.i18n(lang). getPages().get(page);
//			if (null != tpl) {
//				mv = defaultModelAndView(tpl, request);
//				return mv;
//			}
//		}
//		
//		
//		if (null != request.getParameter("edit")) {
//			// Edit mode, redirect pageSize the wiki
//
//			response.sendRedirect(config.getWikiConfig().getEditUrl(vertical,page));
//		} else {
//			// Rendering mode
//
//			WikiResult content = null;
//			try {
//				content = xwikiService.getPage(vertical + "/" + page);
//			} catch (TechnicalException e) {
//				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");	
//			} catch (ResourceNotFoundException e) {
//				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content Not Found");	
//			}
//
//			if (StringUtils.isEmpty(content.getHtml())) {
//				//				mv.setStatus(HttpStatus.NOT_FOUND);
//				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content Not Found");		    
//			} else {
//				mv = defaultModelAndView(("xwiki-"+content.getLayout()), request);
//				mv.addObject("content", content);
//			}
//		}
//
//		return mv;
//	}
>>>>>>> cbcd929 xwiki-spring-boot-starter integration
=======
	}
>>>>>>> 6c3b5b0 Wiki backed pages rendering on the way !


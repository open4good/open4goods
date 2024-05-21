package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.util.Locale;

import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

	private  XwikiFacadeService xwikiService;

	private String[] frags;
	

	public XwikiController(	 ) {
		super();
	}

	public XwikiController(	 XwikiFacadeService xwikiService, 	 String wikiPage) {
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
	

	// TODO : Caching
	// TODO : Mutualize with the one in blog controller (?)
	// TODO : Serve here the classical xwiki download content, because of XwikiController not being @nnotated
	// TODO : Security warning 
	public void attachment( final HttpServletRequest request, HttpServletResponse response) throws IOException  {
		// TODO : Blog
		String path = request.getServletPath().replace(XWikiHtmlService.PROXYFIED_FOLDER+"/", "");
		byte[] bytes = xwikiService.downloadAttachment(path);
		response.setContentType(xwikiService.detectMimeType(path));
		// TODO : Have a streamed version
		response.getOutputStream().write(bytes);
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
//

	}


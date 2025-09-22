package org.open4goods.ui.controllers.ui;

import java.util.Locale;

import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller

/**
 * This controller pages pageSize Xwiki content
 *
 * @author gof TODO : Could put in the xwiki-starter
 *
 */
public class XwikiController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(XwikiController.class);

	private static final String WEBPAGE_CLASS_META_TITLE = "metaTitle";
	private static final String WEBPAGE_CLASS_META_DESCRIPTION = "metaDescription";
	private static final String WEBPAGE_CLASS_PAGE_TITLE = "pageTitle";
	private static final String WEBPAGE_CLASS_HTML = "html";
	private static final String WEBPAGE_CLASS_WIDTH = "width";

	private XwikiFacadeService xwikiService;
	private UiService uiService;

	private String frags;

	private VerticalConfig verticalConfig;

	public XwikiController() {
		super();
	}

	public XwikiController(XwikiFacadeService xwikiService, UiService uiService, String wikiPage, VerticalConfig verticalConfig) {
		super();
		this.xwikiService = xwikiService;
		this.uiService = uiService;
		this.frags = wikiPage;
		this.verticalConfig = verticalConfig;
	}

	public XwikiController(XwikiFacadeService xwikiService, UiService uiService, String key) {
		super();
		this.xwikiService = xwikiService;
		this.uiService = uiService;
		this.frags = key;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO : Temporary  set default domain language
		FullPage fullPage = xwikiService.getFullPage(frags,"en");

		ModelAndView mv = uiService.defaultModelAndView("xwiki-" + fullPage.getProp("layout"), request);

		mv.addObject(WEBPAGE_CLASS_META_TITLE, fullPage.getProp(WEBPAGE_CLASS_META_TITLE));
		mv.addObject(WEBPAGE_CLASS_META_DESCRIPTION, fullPage.getProp(WEBPAGE_CLASS_META_DESCRIPTION));
		mv.addObject(WEBPAGE_CLASS_PAGE_TITLE, fullPage.getProp(WEBPAGE_CLASS_PAGE_TITLE));
		mv.addObject(WEBPAGE_CLASS_HTML, getHtml(fullPage));
		mv.addObject(WEBPAGE_CLASS_WIDTH, fullPage.getProp(WEBPAGE_CLASS_WIDTH));
		mv.addObject("verticalConfig", verticalConfig);
		mv.addObject("editLink", xwikiService.getPathHelper().getEditpath(frags.replace('.', '/')));
		mv.addObject("userLocale", request.getLocale());
		// TODO(i18n,p3) : localisation
		mv.addObject("siteLanguage", "fr");
		final Locale sl = Locale.FRANCE;
		mv.addObject("siteLocale", sl);
		return mv;
	}

	/**
	 * Get the XWIKI corresponding html for a wiki page
	 *
	 * @param fullPage
	 * @return
	 */
	private String getHtml(FullPage fullPage) {
		String ret = xwikiService.getxWikiHtmlService().getHtmlClassWebPage(fullPage.getWikiPage().getId());
		return ret;
	}

}

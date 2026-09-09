package org.open4goods.ui.controllers.ui.pages;

import java.io.IOException;

import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Proxies XWiki-hosted attachments referenced by any XWiki-rendered content served through
 * {@code ui} (not blog-specific: XWikiHtmlService rewrites every {@code /bin/download} link it
 * renders to this path, for editorial pages, legal pages, etc.).
 */
@Controller
public class XwikiProxyController {

	private final XwikiFacadeService xwikiFacadeService;

	public XwikiProxyController(XwikiFacadeService xwikiFacadeService) {
		this.xwikiFacadeService = xwikiFacadeService;
	}

	@GetMapping(XWikiHtmlService.PROXYFIED_FOLDER + "/**")
	// TODO(p3,design) : classical xwiki download content is served here and it shouldn't, because of XwikiController not being @nnotated
	// TODO(p3,perf) : Caching, streamed version
	public void attachment(final HttpServletRequest request, HttpServletResponse response) throws IOException {
		String path = request.getServletPath().replace(XWikiHtmlService.PROXYFIED_FOLDER + "/", "");
		byte[] bytes = xwikiFacadeService.downloadAttachment(path);
		response.setContentType(xwikiFacadeService.detectMimeType(path));
		response.getOutputStream().write(bytes);
	}
}

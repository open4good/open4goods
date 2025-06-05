package org.open4goods.xwiki.services;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.xwiki.config.UrlManagementHelper;
import org.open4goods.xwiki.config.XWikiConstantsResourcesPath;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.open4goods.xwiki.model.FullPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Pages;

/**
 * An Xwiki facade service, which encapsulates xwiki unitary services to deliver
 * high level  wiki content to spring boot web translation
 */
public class XwikiFacadeService {

	private static Logger LOGGER = LoggerFactory.getLogger(XwikiFacadeService.class);

	// The unitary services
	private final XwikiMappingService mappingService;
	private final XWikiReadService xWikiReadService;
	private final XWikiHtmlService xWikiHtmlService;
	private final XWikiObjectService xWikiObjectService;

	private XWikiServiceProperties properties;

	private UrlManagementHelper urlHelper;

	private XWikiConstantsResourcesPath pathHelper;
	

	public XwikiFacadeService( XwikiMappingService mappingService, XWikiObjectService xWikiObjectService, XWikiHtmlService xWikiHtmlService,XWikiReadService xWikiReadService, XWikiObjectService xWikiObjectService2, XWikiHtmlService xWikiHtmlService2, XWikiServiceProperties properties) {
		this.mappingService = mappingService;		
		this.xWikiReadService = xWikiReadService;
		this.xWikiHtmlService = xWikiHtmlService2;
		this.xWikiObjectService = xWikiObjectService2;
		this.properties = properties;
		this.urlHelper = new UrlManagementHelper(properties);
		this.pathHelper = new XWikiConstantsResourcesPath(properties.getBaseUrl(), properties.getApiEntrypoint(), properties.getApiWiki());

	}
	
	// TODO : I18n
	// @Cacheable(cacheNames = XWikiServiceProperties.SPRING_CACHE_NAME)
	public FullPage getFullPage (String restPath) {
		FullPage ret = new FullPage();
		
		String htmlContent = xWikiHtmlService.html(restPath.replaceAll("\\.|:","/"));
		// TODO : When xwiki jakarta compliant
//		String htmlContent = xWikiHtmlService.renderXWiki20SyntaxAsXHTML(wikiPage.getContent());
		
		Page wikiPage  = xWikiReadService.getPage(restPath);
		// TODO : Seems useless
//		Objects objects = mappingService.getPageObjects(wikiPage);
		Map<String, String> properties = xWikiObjectService.getProperties(wikiPage);
	
		ret.setHtmlContent(htmlContent);
		ret.setWikiPage(wikiPage);
//		ret.setObjects(objects);
		ret.setProperties(properties);
		
		
		return ret;		
	}

	public FullPage getFullPage(String space, String name) {
		return getFullPage(space+":"+name);
	}
	
	
	/**
	 * 
	 * @param url
	 * TODO : Should provide a streamed version 
	 * @return
	 */
	public byte[] downloadAttachment( String space, String page, String attachmentName) {
		String url = pathHelper.getDownloadAttachlmentUrl(space, page, attachmentName);		
		return mappingService.downloadAttachment(url);
	}
	
	

	public byte[] downloadAttachment(String string) {
		// TODO : Security
		String url = pathHelper.getDownloadpath() + string;		
		return mappingService.downloadAttachment(url);
	}

	
	public String detectMimeType (String filename) {
        // TODO : ugly, should fetch the meta (mime type is availlable in xwiki service), but does not work for the blog image, special class and not appears in attachments list
		if (filename.endsWith(".pdf")) {
			return("application/pdf");
		} else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			return("image/jpeg");
		} else if (filename.endsWith(".png")) {
			return("image/png");
		} else if (filename.endsWith(".gif")) {
			return("image/gif");
		}else {
			LOGGER.error("Unknown mime type mapping in XwikiFacadeService for : {}",filename);
			return "";
		}
	}
	
	
	

	
	
	
	// TODO : Remove, or be more exaustiv
	public Pages getPages(String path) {
		return xWikiReadService.getPages(path);
	}

	public XwikiMappingService getMappingService() {
		return mappingService;
	}

	public XWikiReadService getxWikiReadService() {
		return xWikiReadService;
	}

	public XWikiHtmlService getxWikiHtmlService() {
		return xWikiHtmlService;
	}

	public XWikiObjectService getxWikiObjectService() {
		return xWikiObjectService;
	}

	public UrlManagementHelper getUrlHelper() {
		return urlHelper;
	}

	public void setUrlHelper(UrlManagementHelper urlHelper) {
		this.urlHelper = urlHelper;
	}

	public XWikiServiceProperties getProperties() {
		return properties;
	}

	public void setProperties(XWikiServiceProperties properties) {
		this.properties = properties;
	}

	public XWikiConstantsResourcesPath getPathHelper() {
		return pathHelper;
	}

	public void setPathHelper(XWikiConstantsResourcesPath pathHelper) {
		this.pathHelper = pathHelper;
	}






}

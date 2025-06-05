package org.open4goods.xwiki.services;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.xwiki.config.UrlManagementHelper;
import org.open4goods.xwiki.config.XWikiConstantsResourcesPath;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rest.model.jaxb.Page;

public class XWikiObjectService {
	
	
	private static Logger LOGGER = LoggerFactory.getLogger(XWikiObjectService.class);
	
	private XWikiServiceProperties xWikiProperties;
	private XWikiConstantsResourcesPath resourcesPathManager;
	private XwikiMappingService mappingService;
	private UrlManagementHelper urlHelper;
	
	public XWikiObjectService (XwikiMappingService mappingService, XWikiServiceProperties xWikiProperties) throws Exception {
		
		this.xWikiProperties = xWikiProperties;
		this.mappingService = mappingService;
		this.resourcesPathManager = new XWikiConstantsResourcesPath(xWikiProperties.getBaseUrl(), xWikiProperties.getApiEntrypoint(), xWikiProperties.getApiWiki());
		this.urlHelper = new UrlManagementHelper(xWikiProperties);
	}
	/**
	 * Get properties from Page 'page'
	 * 
	 * @param page
	 * @return 
	 */
	public Map<String,String> getProperties(Page page) {
		Map<String,String> props = new HashMap<String, String>();
		if(page != null) {
			props = this.mappingService.getProperties(page);
		}
		return props;
	}
	

}

package org.open4goods.ui.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.config.yml.WikiPageConfig;
import org.open4goods.config.yml.ui.ProductI18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.Localisable;
import org.open4goods.services.SearchService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.controllers.ui.VerticalController;
import org.open4goods.ui.controllers.ui.XwikiController;
import org.open4goods.ui.services.BlogService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Configuration
public class UrlHandlerMappingConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(UrlHandlerMappingConfig.class);

	
	@Autowired	UiConfig config;
	@Autowired	XwikiFacadeService xwikiService;
	@Autowired	VerticalsConfigService verticalConfigService;
	@Autowired  UiService uiService;
	@Autowired  VerticalsConfigService verticalService;
	@Autowired  SearchService searchService;
	@Autowired  BlogService blogService;
	
	@Bean
	public SimpleUrlHandlerMapping wikiUrlHandlerMapping() {
		SimpleUrlHandlerMapping wikiUrlHandlerMapping = new SimpleUrlHandlerMapping();

		Map<String, Object> urlMap = new HashMap<>();

		//////////////////////////////////
		// Adding wiki pages mapping		
		/////////////////////////////////
		for (Entry<String, Localisable> item : config.getWikiPagesMapping().entrySet()) {
			for (String localizedValue : item.getValue().values()) {
				String url = localizedValue.startsWith("/") ? localizedValue : "/" + localizedValue;
				// TODO : Forward i18n
				LOGGER.info("Adding wiki page mapping : {}", url);
				urlMap.put(url, new XwikiController(xwikiService, uiService, item.getKey()));
			}
		}
		
		//////////////////////////////////
		// Adding vertical controller pages
		/////////////////////////////////
		for (VerticalConfig item : verticalConfigService.getConfigsWithoutDefault()) {
			for (Entry<String, ProductI18nElements> i18n : item.getI18n().entrySet() ) {
				
				/////////////////
				// Adding vertical home page
				//////////////// 
				// TODO : Forward i18n
				String url = "/" + i18n.getValue().getVerticalHomeUrl();
				LOGGER.info("Adding vertical home page mapping : {}", url);				
				urlMap.put(url, new VerticalController(verticalService, searchService, uiService, item.getId(), blogService)  );
				
				/////////////////
				// Adding vertical specific wikipages
				//////////////// 
				for (WikiPageConfig page : i18n.getValue().getWikiPages()) {
					String pUrl =  "/" + i18n.getValue().getVerticalHomeUrl() + "/" + page.getVerticalUrl();
					LOGGER.info("Adding vertical specific page mapping : {}", url);				
					urlMap.put(pUrl, new  XwikiController(xwikiService, uiService, page.getWikiUrl()));					
				}
				
			}
		}

		wikiUrlHandlerMapping.setUrlMap(urlMap);
		wikiUrlHandlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
		
		return wikiUrlHandlerMapping;
	}

}
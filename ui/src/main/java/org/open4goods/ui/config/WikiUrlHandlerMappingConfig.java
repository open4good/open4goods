package org.open4goods.ui.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.model.Localisable;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.XwikiController;
import org.open4goods.xwiki.services.XWikiReadService;
import org.open4goods.xwiki.services.XwikiMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Configuration
public class WikiUrlHandlerMappingConfig {

	@Autowired UiConfig config;
	@Autowired XwikiMappingService mappingService;
	@Autowired XWikiReadService xwikiService;
	
	@Bean
    public SimpleUrlHandlerMapping wikiUrlHandlerMapping() {
        SimpleUrlHandlerMapping wikiUrlHandlerMapping = new SimpleUrlHandlerMapping();
        
        Map<String, Object> urlMap = new HashMap<>();
        
        // Adding each localised controller
        for (Entry<String, Localisable> item : config.getWikiPagesMapping().entrySet()) {
        	urlMap.put("/simpleUrlWelcome", wikiController());
        	wikiUrlHandlerMapping.setUrlMap(urlMap);        
        }
        return wikiUrlHandlerMapping;
    }

    @Bean
    public XwikiController wikiController() {
        return new XwikiController(config, mappingService, xwikiService);
    }
}
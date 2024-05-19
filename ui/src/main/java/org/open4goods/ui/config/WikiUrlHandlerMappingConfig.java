package org.open4goods.ui.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.model.Localisable;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.XwikiController;
import org.open4goods.xwiki.services.XWikiReadService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.open4goods.xwiki.services.XwikiMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Configuration
public class WikiUrlHandlerMappingConfig {

	@Autowired UiConfig config;
	@Autowired XwikiFacadeService xwikiService;
	
	@Bean
    public SimpleUrlHandlerMapping wikiUrlHandlerMapping() {
        SimpleUrlHandlerMapping wikiUrlHandlerMapping = new SimpleUrlHandlerMapping();
        
        Map<String, Object> urlMap = new HashMap<>();
        
        // Adding each localised controller
        for (Entry<String, Localisable> item : config.getWikiPagesMapping().entrySet()) {
        	for (String localizedValue : item.getValue().values()) {
        		urlMap.put("/"+localizedValue,  new XwikiController( xwikiService, item.getKey()));        		
        	}
        	wikiUrlHandlerMapping.setUrlMap(urlMap);        
        }
        return wikiUrlHandlerMapping;
    }

}
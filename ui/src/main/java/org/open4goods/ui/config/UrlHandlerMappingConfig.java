package org.open4goods.ui.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.commons.services.SearchService;
import org.open4goods.model.Localisable;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.WikiPageConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.CategoryController;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.controllers.ui.VerticalBrandsController;
import org.open4goods.ui.controllers.ui.VerticalBrandsController;
import org.open4goods.ui.controllers.ui.VerticalController;
import org.open4goods.ui.controllers.ui.VerticalSubsetController;
import org.open4goods.ui.controllers.ui.XwikiController;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.services.blog.service.BlogService;
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
	@Autowired  GoogleTaxonomyService googleTaxonomyService;
	@Autowired  SerialisationService serialisationService;


    @Bean
    SimpleUrlHandlerMapping wikiUrlHandlerMapping() {
		SimpleUrlHandlerMapping wikiUrlHandlerMapping = new SimpleUrlHandlerMapping();

		Map<String, Object> urlMap = new HashMap<>();

		//////////////////////////////////
		// Adding wiki pages mapping		
		/////////////////////////////////
		for (Entry<String, Localisable<String,String>> item : config.getWikiPagesMapping().entrySet()) {
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
				String baseUrl = "/" + i18n.getValue().getVerticalHomeUrl();
				LOGGER.info("Adding vertical home page mapping : {}", baseUrl);				
				urlMap.put(baseUrl, new VerticalController(verticalService, searchService, uiService, item.getId(), blogService, serialisationService)  );

				/////////////////
				// Adding ecoscore page
				//////////////// 
				LOGGER.info("Adding vertical ecoscore page mapping : {}", baseUrl);				
				urlMap.put(baseUrl+"/ecoscore", new VerticalEcoscoreController(item, uiService,serialisationService)  );

				
				
				/////////////////
				// Adding subset pages
				//////////////// 
				item.getSubsets().forEach(subset -> {
					LOGGER.info("Adding vertical subset page mapping : {}", subset.getId());
					// TODO(p3,i18n)
					urlMap.put(baseUrl+"/"+subset.getUrl().i18n("fr"), new VerticalSubsetController(verticalService, searchService, uiService, item.getId(), blogService, serialisationService, subset)  );
				});
				
				
				
				
				/////////////////
				// Adding vertical brand pages
				//////////////// 
				// TODO : Forward i18n
				LOGGER.info("Adding vertical brand pages mapping : {}", baseUrl);				
				urlMap.put(baseUrl +"/marques/*", new VerticalBrandsController(verticalService, searchService, uiService, item.getId(), blogService, serialisationService)  );

				
				/////////////////
				// Adding vertical specific wikipages
				//////////////// 
				for (WikiPageConfig page : i18n.getValue().getWikiPages()) {
					String pUrl =  baseUrl+ "/" + page.getVerticalUrl();
					LOGGER.info("Adding vertical specific page mapping : {}", pUrl);				
					urlMap.put(pUrl, new  XwikiController(xwikiService, uiService, page.getWikiUrl(),item));					
				}
				
			}
		}

		//////////////////////////////////////////////////////////
		// Adding all categories path were we have a vertical id
		//////////////////////////////////////////////////////////
		// TODO(P3,i18n) : i18n
		urlMap.put("categories", new CategoryController(uiService, googleTaxonomyService.getCategories().asRootNode(), googleTaxonomyService));
		for (Entry<String, ProductCategory> category: googleTaxonomyService.getCategories().paths("fr").entrySet()) {
			urlMap.put("categories/"+category.getKey(), new CategoryController(uiService, category.getValue(), googleTaxonomyService));
		}
		
		
		
		wikiUrlHandlerMapping.setUrlMap(urlMap);
		wikiUrlHandlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
		
		return wikiUrlHandlerMapping;
	}

}